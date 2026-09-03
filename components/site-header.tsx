"use client"

import { Bot } from "lucide-react"
import Link from "next/link"
import { useAuth } from "@/lib/auth-context"

export function SiteHeader() {
  const { user, loading } = useAuth()

  return (
    <header className="sticky top-0 z-50 border-b border-white/5 bg-neutral-900/70 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 md:px-6">
        <Link href="/" className="flex items-center gap-2.5">
          <Bot className="size-6 text-primary" aria-hidden="true" />
          <span className="text-lg font-semibold tracking-tight text-white">AutoGrabber</span>
        </Link>
        {loading ? (
          <span className="h-8 w-20" aria-hidden="true" />
        ) : user ? (
          <Link
            href="/account"
            className="rounded-md border border-primary/60 px-4 py-2 text-xs font-semibold tracking-widest text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
          >
            ACCOUNT
          </Link>
        ) : (
          <Link
            href="/login"
            className="rounded-md border border-primary/60 px-4 py-2 text-xs font-semibold tracking-widest text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
          >
            SIGN IN
          </Link>
        )}
      </div>
    </header>
  )
}
