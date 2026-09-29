"use client"

import { addDoc, collection, limit, onSnapshot, orderBy, query, serverTimestamp } from "firebase/firestore"
import { FormEvent, useEffect, useMemo, useRef, useState } from "react"
import { MessageCircle, Send, X } from "lucide-react"
import { useAuth } from "@/lib/auth-context"
import { db } from "@/lib/firebase"

 type ChatMessage = { id: string; uid: string; name: string; text: string }

export function ChatBubble() {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [text, setText] = useState("")
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const endRef = useRef<HTMLDivElement>(null)
  const displayName = useMemo(() => user?.displayName || user?.email?.split("@")[0] || "Member", [user])

  useEffect(() => {
    if (!user) return
    const messagesQuery = query(collection(db, "chatMessages"), orderBy("createdAt", "asc"), limit(100))
    return onSnapshot(messagesQuery, (snapshot) => {
      setMessages(snapshot.docs.map((item) => ({ id: item.id, ...item.data() } as ChatMessage)))
    }, () => setError("Chat is temporarily unavailable."))
  }, [user])

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, open])

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = text.trim()
    if (!user || !message || sending) return
    setSending(true)
    setError(null)
    try {
      await addDoc(collection(db, "chatMessages"), { uid: user.uid, name: displayName, text: message, createdAt: serverTimestamp() })
      setText("")
    } catch {
      setError("Your message could not be sent.")
    } finally { setSending(false) }
  }

  if (!user) return null

  return <div className="fixed bottom-5 right-5 z-50 sm:bottom-6 sm:right-6">
    {open && <section aria-label="Community chat" className="mb-3 flex h-[min(520px,calc(100dvh-7rem))] w-[min(380px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-white/10 bg-card shadow-2xl shadow-black/40">
      <header className="flex items-center justify-between border-b border-white/10 px-4 py-3"><div className="flex items-center gap-2"><MessageCircle className="size-4 text-primary" /><div><h2 className="text-sm font-semibold text-white">Community chat</h2><p className="text-[11px] text-muted-foreground">Live AutoGrabber members</p></div></div><button type="button" onClick={() => setOpen(false)} aria-label="Close chat" className="rounded-lg p-2 text-muted-foreground hover:bg-white/5 hover:text-white"><X className="size-4" /></button></header>
      <div className="flex-1 space-y-3 overflow-y-auto p-4">{error ? <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">{error}</p> : messages.length === 0 ? <p className="py-10 text-center text-xs text-muted-foreground">No messages yet. Start the conversation.</p> : messages.map((message) => <article key={message.id} className={message.uid === user.uid ? "ml-auto max-w-[85%]" : "max-w-[85%]"}><p className="mb-1 px-1 text-[11px] text-muted-foreground">{message.uid === user.uid ? "You" : message.name}</p><p className={message.uid === user.uid ? "rounded-2xl rounded-br-md bg-primary px-3 py-2 text-sm leading-5 text-primary-foreground" : "rounded-2xl rounded-bl-md border border-white/10 bg-background/70 px-3 py-2 text-sm leading-5 text-white"}>{message.text}</p></article>)}<div ref={endRef} /></div>
      <form onSubmit={sendMessage} className="border-t border-white/10 p-3"><div className="flex items-end gap-2"><label htmlFor="chat-bubble-message" className="sr-only">Message</label><textarea id="chat-bubble-message" value={text} onChange={(event) => setText(event.target.value)} placeholder="Write a message…" rows={1} className="min-h-10 flex-1 resize-none rounded-lg border border-white/10 bg-background px-3 py-2.5 text-sm text-white outline-none placeholder:text-muted-foreground focus:border-primary" /><button type="submit" disabled={sending || !text.trim()} aria-label="Send message" className="rounded-lg bg-primary p-2.5 text-primary-foreground disabled:opacity-50"><Send className="size-4" /></button></div></form>
    </section>}
    <button type="button" onClick={() => setOpen((value) => !value)} aria-label={open ? "Close community chat" : "Open community chat"} className="ml-auto flex size-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:scale-105"><MessageCircle className="size-6" /></button>
  </div>
}
