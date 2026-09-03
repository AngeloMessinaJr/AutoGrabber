"use client"

import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signOut as fbSignOut,
  signInWithEmailAndPassword,
  updateProfile,
  type User,
} from "firebase/auth"
import { createContext, useContext, useEffect, useMemo, useState } from "react"
import { auth } from "@/lib/firebase"

type AuthContextValue = {
  user: User | null
  loading: boolean
  signUp: (email: string, password: string, displayName?: string) => Promise<void>
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
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
      async signUp(email, password, displayName) {
        const cred = await createUserWithEmailAndPassword(auth, email, password)
        if (displayName) {
          await updateProfile(cred.user, { displayName })
          setUser({ ...cred.user })
        }
      },
      async signIn(email, password) {
        await signInWithEmailAndPassword(auth, email, password)
      },
      async signOut() {
        await fbSignOut(auth)
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
