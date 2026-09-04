"use client"

import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signOut as fbSignOut,
  signInWithEmailAndPassword,
  EmailAuthProvider,
  reauthenticateWithCredential,
  deleteUser,
  updateProfile,
  type User,
} from "firebase/auth"
import { createContext, useContext, useEffect, useMemo, useState } from "react"
import { deleteDoc, doc, setDoc } from "firebase/firestore"
import { auth, db } from "@/lib/firebase"

type AuthContextValue = {
  user: User | null
  loading: boolean
  signUp: (form: { email: string; password: string; fullName?: string; phoneNumber?: string; dateOfBirth?: string }) => Promise<void>
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  deleteAccount: (password: string) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (u) => {
      setUser(u)
      setLoading(false)
    })
    return () => unsubscribe()
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      async signUp(form) {
        const cred = await createUserWithEmailAndPassword(auth, form.email, form.password)
        const fullName = form.fullName || ""
        if (fullName) await updateProfile(cred.user, { displayName: fullName })
        await setDoc(doc(db, "users", cred.user.uid), {
          id: cred.user.uid,
          email: cred.user.email,
          fullName,
          phoneNumber: form.phoneNumber || "",
          dateOfBirth: form.dateOfBirth || "",
          homeAddress: "",
          profilePictureUrl: "",
          approved: true,
          hasLifetimeAccess: false,
          status: "active",
          registrationDate: Date.now(),
          purchasedAt: null,
        }, { merge: true })
      },
      async signIn(email, password) {
        await signInWithEmailAndPassword(auth, email, password)
      },
      async signOut() {
        await fbSignOut(auth)
      },
      async deleteAccount(password) {
        if (!auth.currentUser || !auth.currentUser.email) throw new Error("No signed-in account found.")
        const credential = EmailAuthProvider.credential(auth.currentUser.email, password)
        await reauthenticateWithCredential(auth.currentUser, credential)
        const uid = auth.currentUser.uid
        await deleteDoc(doc(db, "users", uid))
        await deleteUser(auth.currentUser)
      },
    }),
    [user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider")
  return ctx
}
