"use client"

import { Bot, Menu, X } from "lucide-react"
import Link from "next/link"
import { useState } from "react"
import { useAuth } from "@/lib/auth-context"

export function SiteHeader() {
  const { user, loading } = useAuth()
  const [open, setOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-background/85 backdrop-blur-xl">
      <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-5 lg:px-8">
        <Link href="/" className="flex items-center gap-3" onClick={() => setOpen(false)}>
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-[0_0_24px_oklch(0.82_0.13_205/0.25)]"><Bot className="size-5" /></span>
          <span className="font-semibold tracking-tight text-foreground">AutoGrabber</span>
        </Link>
        <nav className="hidden items-center gap-8 text-sm text-muted-foreground md:flex" aria-label="Main navigation">
          <Link href="/features" className="transition-colors hover:text-primary">Features</Link>
          <Link href="/faq" className="transition-colors hover:text-primary">FAQ</Link>
          <Link href="/contact" className="transition-colors hover:text-primary">Contact</Link>
        </nav>
        <div className="hidden md:block">
          {!loading && (user ? <Link href="/account" className="rounded-lg bg-primary px-4 py-2.5 text-xs font-bold tracking-widest text-primary-foreground transition-opacity hover:opacity-90">ACCOUNT</Link> : <Link href="/login" className="rounded-lg border border-primary/60 px-4 py-2.5 text-xs font-bold tracking-widest text-primary transition-colors hover:bg-primary hover:text-primary-foreground">SIGN IN</Link>)}
        </div>
        <div className="flex items-center gap-2 md:hidden">
          {!loading && (user ? (
            <Link href="/account" className="rounded-lg bg-primary px-3 py-2 text-[11px] font-bold tracking-wider text-primary-foreground" aria-label="Open account">ACCOUNT</Link>
          ) : (
            <Link href="/login" className="rounded-lg border border-primary/60 px-3 py-2 text-[11px] font-bold tracking-wider text-primary" aria-label="Sign in">SIGN IN</Link>
          ))}
          <button type="button" className="p-2 text-foreground" onClick={() => setOpen(!open)} aria-label={open ? "Close menu" : "Open menu"}>{open ? <X /> : <Menu />}</button>
        </div>
      </div>
      {open && <nav className="flex flex-col gap-5 border-t border-white/10 px-5 py-6 text-sm text-muted-foreground md:hidden" aria-label="Mobile navigation">
        <Link href="/features" onClick={() => setOpen(false)}>Features</Link><Link href="/faq" onClick={() => setOpen(false)}>FAQ</Link><Link href="/contact" onClick={() => setOpen(false)}>Contact</Link>
      </nav>}
    </header>
  )
}
