"use client"

import { Bot, Loader2 } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth-context"
import { authErrorMessage } from "@/lib/auth-errors"

export default function LoginPage() {
  const router = useRouter()
  const { user, loading, signIn, signUp } = useAuth()
  const [mode, setMode] = useState<"signin" | "signup">("signin")
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!loading && user) router.replace("/account")
  }, [loading, user, router])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError("")
    setSubmitting(true)
    try {
      if (mode === "signup") {
        await signUp(email, password, name.trim() || undefined)
      } else {
        await signIn(email, password)
      }
      router.replace("/account")
    } catch (err) {
      setError(authErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-4 py-12">
      <Link href="/" className="mb-8 flex items-center gap-2.5">
        <Bot className="size-6 text-primary" aria-hidden="true" />
        <span className="text-lg font-semibold tracking-tight text-white">AutoGrabber</span>
      </Link>

      <div className="w-full max-w-sm rounded-2xl border border-white/8 bg-card/60 p-6 md:p-8">
        <h1 className="text-xl font-semibold text-white">
          {mode === "signin" ? "Welcome back" : "Create your account"}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {mode === "signin"
            ? "Sign in to manage your AutoGrabber account."
            : "Sign up to get started with AutoGrabber."}
        </p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
          {mode === "signup" && (
            <div className="flex flex-col gap-1.5">
              <label htmlFor="name" className="text-xs font-medium text-white/80">
                Name
              </label>
              <input
                id="name"
                type="text"
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="rounded-lg border border-input bg-white/[0.03] px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60"
                placeholder="Jane Driver"
              />
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <label htmlFor="email" className="text-xs font-medium text-white/80">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="rounded-lg border border-input bg-white/[0.03] px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60"
              placeholder="you@example.com"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="password" className="text-xs font-medium text-white/80">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={6}
              autoComplete={mode === "signin" ? "current-password" : "new-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="rounded-lg border border-input bg-white/[0.03] px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60"
              placeholder="••••••••"
            />
          </div>

          {error && (
            <p role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="flex items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-60"
          >
            {submitting && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
            {mode === "signin" ? "Sign in" : "Create account"}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          {mode === "signin" ? "Don't have an account?" : "Already have an account?"}{" "}
          <button
            type="button"
            onClick={() => {
              setMode(mode === "signin" ? "signup" : "signin")
              setError("")
            }}
            className="font-semibold text-primary hover:underline"
          >
            {mode === "signin" ? "Sign up" : "Sign in"}
          </button>
        </p>
      </div>
    </main>
  )
}
