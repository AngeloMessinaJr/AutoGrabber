"use client"

import { doc, onSnapshot, setDoc } from "firebase/firestore"
import { AlertTriangle, Bot, Check, CreditCard, LogOut, Mail, Pencil, Phone, ShieldCheck, User as UserIcon, X } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Suspense, useEffect, useState } from "react"
import { useAuth } from "@/lib/auth-context"
import { db } from "@/lib/firebase"
import { authErrorMessage } from "@/lib/auth-errors"

type UserProfile = {
  fullName?: string
  phoneNumber?: string
  profilePictureUrl?: string
  hasLifetimeAccess?: boolean
}

function AccountContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { user, loading, signOut, deleteAccount } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [purchaseMessage, setPurchaseMessage] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)

  useEffect(() => {
    if (!loading && !user) router.replace("/login")
  }, [loading, user, router])

  useEffect(() => {
    if (!user) {
      setProfile(null)
      return
    }
    const unsubscribe = onSnapshot(doc(db, "users", user.uid), (snap) => {
      setProfile(snap.exists() ? (snap.data() as UserProfile) : {})
    })
    return () => unsubscribe()
  }, [user])

  useEffect(() => {
    const purchase = searchParams.get("purchase")
    const sessionId = searchParams.get("session_id")
    if (!user || loading || purchase !== "success" || !sessionId) return

    let cancelled = false
    async function verifyPurchase() {
      try {
        const token = await user!.getIdToken()
        const response = await fetch("/api/checkout/verify", {
          method: "POST",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
          body: JSON.stringify({ session_id: sessionId }),
        })
        if (!response.ok) throw new Error("Payment verification failed")
        if (!cancelled) setPurchaseMessage("Payment confirmed. Lifetime access is now active.")
      } catch {
        if (!cancelled) setPurchaseMessage("Payment received. Access will activate automatically once Stripe finishes processing.")
      }
    }
    void verifyPurchase()
    return () => {
      cancelled = true
    }
  }, [loading, searchParams, user])

  async function handleSignOut() {
    await signOut()
    router.replace("/login")
  }

  if (loading || !user) {
    return (
      <main className="flex min-h-dvh items-center justify-center">
        <p className="text-sm text-muted-foreground">Loading…</p>
      </main>
    )
  }

  const displayName = profile?.fullName || user.displayName || user.email?.split("@")[0] || "Driver"
  const initial = displayName.charAt(0).toUpperCase()
  const profilePictureUrl = profile?.profilePictureUrl
  const hasLifetimeAccess = profile?.hasLifetimeAccess === true
  const joined = user.metadata?.creationTime
    ? new Date(user.metadata.creationTime).toLocaleDateString(undefined, {
        year: "numeric",
        month: "long",
        day: "numeric",
      })
    : "—"

  return (
    <main className="min-h-dvh">
      <header className="border-b border-white/5 bg-neutral-900/70 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-4xl items-center justify-between px-4 md:px-6">
          <Link href="/" className="flex items-center gap-2.5">
            <Bot className="size-6 text-primary" aria-hidden="true" />
            <span className="text-lg font-semibold tracking-tight text-white">AutoGrabber</span>
          </Link>
          <button
            type="button"
            onClick={handleSignOut}
            className="flex items-center gap-2 rounded-md border border-white/10 px-3 py-2 text-xs font-semibold text-white/80 transition-colors hover:bg-white/5"
          >
            <LogOut className="size-4" aria-hidden="true" />
            Sign out
          </button>
        </div>
      </header>

      <div className="mx-auto max-w-4xl px-4 py-10 md:px-6 md:py-14">
        <div className="flex items-center gap-4">
          <div className="relative shrink-0">
            {profilePictureUrl ? (
              <Image
                src={profilePictureUrl || "/placeholder.svg"}
                alt={displayName}
                width={64}
                height={64}
                unoptimized
                className="size-16 rounded-full object-cover"
              />
            ) : (
              <div className="flex size-16 items-center justify-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
                {initial}
              </div>
            )}
            <button
              type="button"
              onClick={() => setEditing(true)}
              aria-label="Edit profile picture"
              className="absolute -bottom-1 -right-1 flex size-7 items-center justify-center rounded-full border border-white/10 bg-neutral-900 text-white/80 transition-colors hover:bg-neutral-800 hover:text-white"
            >
              <Pencil className="size-3.5" aria-hidden="true" />
            </button>
          </div>
          <div>
            <h1 className="text-2xl font-semibold text-white">{displayName}</h1>
            <p className="text-sm text-muted-foreground">{user.email}</p>
          </div>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <InfoCard
            icon={<UserIcon className="size-4" />}
            label="Full Name"
            value={profile?.fullName || user.displayName || "Not set"}
            onEdit={() => setEditing(true)}
          />
          <InfoCard icon={<Mail className="size-4" />} label="Email" value={user.email || "—"} />
          <InfoCard
            icon={<Phone className="size-4" />}
            label="Phone number"
            value={profile?.phoneNumber || "Not set"}
            onEdit={() => setEditing(true)}
          />
          <InfoCard
            icon={<ShieldCheck className="size-4" />}
            label="Email verified"
            value={user.emailVerified ? "Yes" : "No"}
          />
          <InfoCard icon={<UserIcon className="size-4" />} label="Member since" value={joined} />
        </div>

        {purchaseMessage && (
          <p role="status" className="mt-6 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
            {purchaseMessage}
          </p>
        )}
        <SubscriptionPortal uid={user.uid} hasLifetimeAccess={hasLifetimeAccess} />

        <div className="mt-8 rounded-2xl border border-destructive/25 bg-destructive/5 p-6 md:p-8">
          <div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 size-5 shrink-0 text-destructive" /><div><h2 className="text-lg font-semibold text-white">Delete account</h2><p className="mt-1 text-sm leading-6 text-muted-foreground">Permanently remove your Firebase account and profile. This also frees your email to register again.</p><button type="button" onClick={() => setDeleting(true)} className="mt-5 rounded-lg border border-destructive/50 px-4 py-2.5 text-sm font-semibold text-destructive transition-colors hover:bg-destructive/10">Delete my account</button></div></div>
        </div>

        <div className="mt-8 rounded-2xl border border-white/8 bg-card/60 p-6 md:p-8">
          <h2 className="text-lg font-semibold text-white">Your download</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Your account is active. Grab the latest AutoGrabber APK below.
          </p>
          <a
            href="/#download"
            className="mt-5 inline-flex items-center justify-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
          >
            Go to download
          </a>
        </div>
      </div>

      {editing && (
        <EditProfileDialog uid={user.uid} profile={profile ?? {}} onClose={() => setEditing(false)} />
      )}
      {deleting && <DeleteAccountDialog email={user.email ?? ""} onClose={() => setDeleting(false)} onDelete={async (password) => { await deleteAccount(password); router.replace("/") }} />}
    </main>
  )
}

export default function AccountPage() {
  return (
    <Suspense fallback={<main className="flex min-h-dvh items-center justify-center"><p className="text-sm text-muted-foreground">Loading…</p></main>}>
      <AccountContent />
    </Suspense>
  )
}

function SubscriptionPortal({ uid, hasLifetimeAccess }: { uid: string; hasLifetimeAccess: boolean }) {
  const { user } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function purchase() {
    if (!user) return
    setLoading(true)
    setError(null)
    try {
      const token = await user.getIdToken()
      const response = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ uid }),
      })
      const contentType = response.headers.get("content-type") ?? ""
      const responseText = await response.text()
      let data: { url?: string; error?: string } = {}
      if (responseText && contentType.includes("application/json")) {
        try {
          data = JSON.parse(responseText)
        } catch {
          throw new Error("The checkout service returned an invalid response. Please try again.")
        }
      }
      if (!response.ok || !data.url) {
        throw new Error(data.error || `Checkout request failed (${response.status}). Please try again.`)
      }
      window.location.assign(data.url)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to start checkout.")
      setLoading(false)
    }
  }

  return (
    <section className="mt-8 rounded-2xl border border-primary/20 bg-primary/5 p-6 md:p-8" aria-labelledby="subscription-title">
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2 text-primary"><CreditCard className="size-4" aria-hidden="true" /><span className="text-xs font-semibold uppercase tracking-widest">Subscription</span></div>
          <h2 id="subscription-title" className="mt-2 text-xl font-semibold text-white">Lifetime access</h2>
          <p className="mt-1 max-w-xl text-sm leading-6 text-muted-foreground">Unlock every AutoGrabber feature with one secure, one-time payment.</p>
        </div>
        {hasLifetimeAccess ? (
          <div className="flex shrink-0 items-center gap-2 rounded-lg border border-primary/30 bg-primary/10 px-4 py-3 text-sm font-semibold text-primary"><Check className="size-4" aria-hidden="true" /> Active</div>
        ) : (
          <button type="button" onClick={purchase} disabled={loading} className="shrink-0 rounded-lg bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-60">{loading ? "Opening checkout…" : "Unlock for $9.99"}</button>
        )}
      </div>
      {!hasLifetimeAccess && <p className="mt-4 text-xs text-muted-foreground">One-time payment. No recurring charges.</p>}
      {error && <p role="alert" className="mt-3 text-sm text-destructive">{error}</p>}
    </section>
  )
}

function EditProfileDialog({
  uid,
  profile,
  onClose,
}: {
  uid: string
  profile: UserProfile
  onClose: () => void
}) {
  const [fullName, setFullName] = useState(profile.fullName ?? "")
  const [phoneNumber, setPhoneNumber] = useState(profile.phoneNumber ?? "")
  const [profilePictureUrl, setProfilePictureUrl] = useState(profile.profilePictureUrl ?? "")
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await setDoc(
        doc(db, "users", uid),
        {
          fullName: fullName.trim(),
          phoneNumber: phoneNumber.trim(),
          profilePictureUrl: profilePictureUrl.trim(),
        },
        { merge: true },
      )
      onClose()
    } catch {
      setError("Could not save changes. Please try again.")
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" aria-label="Close" onClick={onClose} className="absolute inset-0 bg-black/70" />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-profile-title"
        className="relative w-full max-w-md rounded-2xl border border-white/10 bg-card p-6 shadow-xl"
      >
        <div className="flex items-center justify-between">
          <h2 id="edit-profile-title" className="text-lg font-semibold text-white">
            Edit profile
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close dialog"
            className="rounded-md p-1 text-muted-foreground transition-colors hover:bg-white/5 hover:text-white"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 flex flex-col gap-4">
          <Field label="Full name" htmlFor="fullName">
            <input
              id="fullName"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Your name"
              className="w-full rounded-lg border border-white/10 bg-background px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
            />
          </Field>

          <Field label="Phone number" htmlFor="phoneNumber">
            <input
              id="phoneNumber"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="+1 (555) 123-4567"
              className="w-full rounded-lg border border-white/10 bg-background px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
            />
          </Field>

          <Field label="Profile picture URL" htmlFor="profilePictureUrl">
            <input
              id="profilePictureUrl"
              type="url"
              value={profilePictureUrl}
              onChange={(e) => setProfilePictureUrl(e.target.value)}
              placeholder="https://example.com/photo.jpg"
              className="w-full rounded-lg border border-white/10 bg-background px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
            />
          </Field>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="mt-1 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-white/10 px-4 py-2.5 text-sm font-semibold text-white/80 transition-colors hover:bg-white/5"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function DeleteAccountDialog({ email, onClose, onDelete }: { email: string; onClose: () => void; onDelete: (password: string) => Promise<void> }) {
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try { await onDelete(password) } catch (err) { setError(authErrorMessage(err)); setSaving(false) }
  }
  return <div className="fixed inset-0 z-50 flex items-center justify-center p-4"><button type="button" aria-label="Close" onClick={onClose} className="absolute inset-0 bg-black/70" /><form onSubmit={submit} role="dialog" aria-modal="true" aria-labelledby="delete-account-title" className="relative w-full max-w-md rounded-2xl border border-destructive/30 bg-card p-6 shadow-xl"><div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 size-5 shrink-0 text-destructive" /><div><h2 id="delete-account-title" className="text-lg font-semibold text-white">Delete account permanently?</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">This removes <span className="text-white">{email}</span> and its profile. You can register this email again afterward.</p></div></div><label htmlFor="delete-password" className="mt-6 block text-xs font-medium uppercase tracking-wide text-muted-foreground">Confirm password</label><input id="delete-password" type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} className="mt-2 w-full rounded-lg border border-input bg-white/[0.03] px-3 py-2.5 text-sm text-white outline-none focus:border-destructive/60" />{error && <p role="alert" className="mt-3 text-sm text-destructive">{error}</p>}<div className="mt-6 flex justify-end gap-3"><button type="button" onClick={onClose} className="rounded-lg border border-white/10 px-4 py-2.5 text-sm text-muted-foreground hover:bg-white/5">Cancel</button><button type="submit" disabled={saving} className="rounded-lg bg-destructive px-4 py-2.5 text-sm font-semibold text-destructive-foreground disabled:opacity-60">{saving ? "Deleting…" : "Delete account"}</button></div></form></div>
}

function Field({ label, htmlFor, children }: { label: string; htmlFor: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </label>
      {children}
    </div>
  )
}

function InfoCard({ icon, label, value, onEdit }: { icon: React.ReactNode; label: string; value: string; onEdit?: () => void }) {
  return (
    <div className="rounded-2xl border border-white/8 bg-card/60 p-5">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-muted-foreground">
          <span className="text-primary" aria-hidden="true">{icon}</span>
          <span className="text-xs font-medium uppercase tracking-wide">{label}</span>
        </div>
        {onEdit && (
          <button type="button" onClick={onEdit} aria-label={`Edit ${label}`} className="rounded-md p-1 text-muted-foreground transition-colors hover:bg-white/5 hover:text-white">
            <Pencil className="size-3.5" aria-hidden="true" />
          </button>
        )}
      </div>
      <p className="mt-2 truncate text-sm font-medium text-white">{value}</p>
    </div>
  )
}
