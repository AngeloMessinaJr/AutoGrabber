"use client"

import { FormEvent, useEffect, useMemo, useRef, useState } from "react"
import { MessageCircle, Reply, Send, X } from "lucide-react"
import { useAuth } from "@/lib/auth-context"
import { auth } from "@/lib/firebase"

type ChatMessage = { id: string; uid: string; name: string; avatarUrl?: string | null; text: string; createdAt: number | null; replyTo?: { id: string; name: string; text: string } | null }
type TypingUser = { uid: string; name: string; avatarUrl?: string | null }

function Avatar({ name, url }: { name: string; url?: string | null }) {
  return url ? <img src={url} alt="" className="size-7 shrink-0 rounded-full object-cover" /> : <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/20 text-xs font-bold text-primary">{name.charAt(0).toUpperCase()}</span>
}

export function ChatBubble() {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [view, setView] = useState<"chat" | "contact">("chat")
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [typingUsers, setTypingUsers] = useState<TypingUser[]>([])
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null)
  const [text, setText] = useState("")
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const endRef = useRef<HTMLDivElement>(null)
  const displayName = useMemo(() => user?.displayName || user?.email?.split("@")[0] || "Member", [user])

  async function request(body?: Record<string, unknown>) {
    const token = await auth.currentUser?.getIdToken()
    if (!token) throw new Error("Authentication required.")
    const response = await fetch("/api/chat", { method: body ? "POST" : "GET", headers: { Authorization: `Bearer ${token}`, ...(body ? { "Content-Type": "application/json" } : {}) }, body: body ? JSON.stringify(body) : undefined, cache: "no-store" })
    if (!response.ok) throw new Error()
    return response.json()
  }

  useEffect(() => {
    if (!user || !open || view !== "chat") return
    let active = true
    async function loadMessages() { try { const data = await request(); if (active) { setMessages(data.messages); setTypingUsers(data.typing || []); setError(null) } } catch { if (active) setError("Chat is temporarily unavailable.") } }
    void loadMessages(); const interval = window.setInterval(loadMessages, 2500)
    return () => { active = false; window.clearInterval(interval); void request({ typing: false }).catch(() => {}) }
  }, [user, open, view])

  useEffect(() => { if (open) endRef.current?.scrollIntoView({ behavior: "smooth" }) }, [messages, open])
  useEffect(() => { if (!text.trim() || !open || view !== "chat") return; const timeout = window.setTimeout(() => void request({ typing: true }).catch(() => {}), 200); return () => window.clearTimeout(timeout) }, [text, open, view])

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const message = text.trim(); if (!user || !message || sending) return
    setSending(true); setError(null)
    try { const data = await request({ text: message, replyTo: replyTo ? { id: replyTo.id, name: replyTo.name, text: replyTo.text } : null }); setMessages((current) => [...current, data.message]); setText(""); setReplyTo(null) } catch { setError("Your message could not be sent.") } finally { setSending(false) }
  }

  if (!user) return null
  const typingLabel = typingUsers.length === 1 ? `${typingUsers[0].name} is typing…` : typingUsers.length > 1 ? `${typingUsers.slice(0, 2).map((item) => item.name).join(" and ")} are typing…` : ""
  return <div className="fixed bottom-5 right-5 z-50 sm:bottom-6 sm:right-6">
    {open && <section aria-label="Community chat" className="mb-3 flex h-[min(520px,calc(100dvh-7rem))] w-[min(380px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-white/10 bg-card shadow-2xl shadow-black/40">
      <header className="border-b border-white/10 px-4 py-3"><div className="flex items-center justify-between"><div className="flex items-center gap-2"><MessageCircle className="size-4 text-primary" /><h2 className="text-sm font-semibold text-white">AutoGrabber Community & Support</h2></div><button type="button" onClick={() => setOpen(false)} aria-label="Close chat" className="rounded-lg p-2 text-muted-foreground hover:bg-white/5 hover:text-white"><X className="size-4" /></button></div><div className="mt-3 flex gap-1 rounded-lg bg-background/60 p-1"><button type="button" onClick={() => setView("chat")} className={`flex-1 rounded-md px-2 py-1.5 text-xs font-semibold ${view === "chat" ? "bg-primary text-primary-foreground" : "text-muted-foreground"}`}>Community Chat</button><button type="button" onClick={() => setView("contact")} className={`flex-1 rounded-md px-2 py-1.5 text-xs font-semibold ${view === "contact" ? "bg-primary text-primary-foreground" : "text-muted-foreground"}`}>Contact Support</button></div></header>
      {view === "contact" ? <div className="flex-1 overflow-y-auto p-4"><p className="text-sm leading-6 text-muted-foreground">Need help with setup, access, billing, or your account? Email us and include the email on your AutoGrabber account.</p><a href="mailto:support@autograbber.app" className="mt-4 block rounded-xl border border-white/10 bg-background/60 p-4 text-sm font-semibold text-primary hover:border-primary/50">support@autograbber.app</a><a href="/faq" className="mt-3 block text-sm font-semibold text-primary hover:underline">Read the FAQ first →</a></div> : <><div className="flex-1 space-y-3 overflow-y-auto p-4">{error ? <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">{error}</p> : messages.length === 0 ? <p className="py-10 text-center text-xs text-muted-foreground">No messages yet. Start the conversation.</p> : messages.map((message) => <article key={message.id} className={`group flex gap-2 ${message.uid === user.uid ? "ml-auto max-w-[90%] flex-row-reverse" : "max-w-[90%]"}`}><Avatar name={message.name} url={message.avatarUrl} /><div className={message.uid === user.uid ? "text-right" : ""}><p className="mb-1 px-1 text-[11px] text-muted-foreground">{message.uid === user.uid ? displayName : message.name}</p>{message.replyTo && <div className="mb-1 rounded-lg border-l-2 border-primary/60 bg-background/60 px-2 py-1 text-left text-[10px] text-muted-foreground">Replying to {message.replyTo.name}: {message.replyTo.text}</div>}<div className="flex items-center gap-1"><p className={message.uid === user.uid ? "rounded-2xl rounded-br-md bg-primary px-3 py-2 text-left text-sm leading-5 text-primary-foreground" : "rounded-2xl rounded-bl-md border border-white/10 bg-background/70 px-3 py-2 text-left text-sm leading-5 text-white"}>{message.text}</p><button type="button" onClick={() => setReplyTo(message)} aria-label={`Reply to ${message.name}`} className="p-1 text-muted-foreground opacity-0 transition-opacity hover:text-primary group-hover:opacity-100"><Reply className="size-3.5" /></button></div></div></article>)}<div ref={endRef} /></div><div className="min-h-5 px-4 text-[11px] text-muted-foreground">{typingLabel}</div><form onSubmit={sendMessage} className="border-t border-white/10 p-3">{replyTo && <div className="mb-2 flex items-center justify-between rounded-lg bg-background/70 px-2.5 py-2 text-xs text-muted-foreground"><span>Replying to {replyTo.name}</span><button type="button" onClick={() => setReplyTo(null)} className="text-primary">Cancel</button></div>}<div className="flex items-end gap-2"><label htmlFor="chat-bubble-message" className="sr-only">Message</label><textarea id="chat-bubble-message" value={text} onChange={(event) => setText(event.target.value)} placeholder="Write a message…" rows={1} className="min-h-10 flex-1 resize-none rounded-lg border border-white/10 bg-background px-3 py-2.5 text-sm text-white outline-none placeholder:text-muted-foreground focus:border-primary" /><button type="submit" disabled={sending || !text.trim()} aria-label="Send message" className="rounded-lg bg-primary p-2.5 text-primary-foreground disabled:opacity-50"><Send className="size-4" /></button></div></form></>}</section>}
    <button type="button" onClick={() => setOpen((value) => !value)} aria-label={open ? "Close community chat" : "Open community chat"} className="ml-auto flex size-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:scale-105"><MessageCircle className="size-6" /></button>
  </div>
}
