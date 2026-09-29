"use client"

import { addDoc, collection, limit, onSnapshot, orderBy, query, serverTimestamp } from "firebase/firestore"
import { FormEvent, useEffect, useMemo, useRef, useState } from "react"
import { ArrowLeft, MessageCircle, Send } from "lucide-react"
import Link from "next/link"
import { useAuth } from "@/lib/auth-context"
import { db } from "@/lib/firebase"

type ChatMessage = {
  id: string
  uid: string
  name: string
  text: string
  createdAt?: { toMillis?: () => number } | null
}

export default function ChatPage() {
  const { user, loading } = useAuth()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [text, setText] = useState("")
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!user) return
    const messagesQuery = query(collection(db, "chatMessages"), orderBy("createdAt", "asc"), limit(100))
    return onSnapshot(messagesQuery, (snapshot) => {
      setMessages(snapshot.docs.map((item) => ({ id: item.id, ...item.data() } as ChatMessage)))
    }, () => setError("Chat is temporarily unavailable. Please try again."))
  }, [user])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const displayName = useMemo(() => user?.displayName || user?.email?.split("@")[0] || "Member", [user])

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = text.trim()
    if (!user || !message || sending) return
    setSending(true)
    setError(null)
    try {
      await addDoc(collection(db, "chatMessages"), {
        uid: user.uid,
        name: displayName,
        text: message,
        createdAt: serverTimestamp(),
      })
      setText("")
    } catch {
      setError("Your message could not be sent. Please try again.")
    } finally {
      setSending(false)
    }
  }

  if (loading) return <main className="min-h-dvh bg-background" />
  if (!user) return <main className="flex min-h-dvh items-center justify-center bg-background px-5"><div className="w-full max-w-md rounded-2xl border border-white/10 bg-card/60 p-8 text-center"><MessageCircle className="mx-auto size-10 text-primary" /><h1 className="mt-5 text-2xl font-semibold text-white">Join the community chat</h1><p className="mt-3 text-sm leading-6 text-muted-foreground">Sign in to chat with other AutoGrabber members in realtime.</p><Link href="/login" className="mt-6 inline-flex rounded-lg bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground">Sign in to continue</Link></div></main>

  return <main className="min-h-dvh bg-background px-4 py-6 sm:px-6"><div className="mx-auto flex min-h-[calc(100dvh-3rem)] max-w-3xl flex-col rounded-2xl border border-white/10 bg-card/40 shadow-2xl shadow-black/20"><header className="flex items-center gap-4 border-b border-white/10 px-5 py-4 sm:px-6"><Link href="/" aria-label="Back home" className="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-white/5 hover:text-white"><ArrowLeft className="size-5" /></Link><div><div className="flex items-center gap-2"><MessageCircle className="size-5 text-primary" /><h1 className="text-lg font-semibold text-white">Community chat</h1></div><p className="mt-1 text-xs text-muted-foreground">Realtime conversation for AutoGrabber members</p></div></header><section aria-label="Chat messages" className="flex-1 space-y-4 overflow-y-auto p-5 sm:p-6">{messages.length === 0 ? <div className="flex h-full min-h-64 items-center justify-center text-center"><div><MessageCircle className="mx-auto size-8 text-primary/70" /><p className="mt-3 text-sm text-muted-foreground">No messages yet. Start the conversation.</p></div></div> : messages.map((message) => <article key={message.id} className={message.uid === user.uid ? "ml-auto max-w-[85%]" : "max-w-[85%]"}><p className="mb-1 px-1 text-xs font-medium text-muted-foreground">{message.uid === user.uid ? "You" : message.name}</p><div className={message.uid === user.uid ? "rounded-2xl rounded-br-md bg-primary px-4 py-3 text-sm leading-6 text-primary-foreground" : "rounded-2xl rounded-bl-md border border-white/10 bg-background/70 px-4 py-3 text-sm leading-6 text-white"}>{message.text}</div></article>)}<div ref={messagesEndRef} /></section><form onSubmit={sendMessage} className="border-t border-white/10 p-4 sm:p-5"><label htmlFor="chat-message" className="sr-only">Message</label><div className="flex items-end gap-3"><textarea id="chat-message" value={text} onChange={(event) => setText(event.target.value)} placeholder="Write a message…" rows={1} maxLength={1000} className="min-h-12 flex-1 resize-none rounded-xl border border-white/10 bg-background px-4 py-3 text-sm leading-6 text-white outline-none placeholder:text-muted-foreground focus:border-primary" onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing && event.keyCode !== 229) { event.preventDefault(); event.currentTarget.form?.requestSubmit() } }} /><button type="submit" disabled={!text.trim() || sending} aria-label="Send message" className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"><Send className="size-5" /></button></div>{error && <p role="alert" className="mt-2 text-xs text-destructive">{error}</p>}<p className="mt-2 text-[11px] text-muted-foreground">Press Enter to send. Shift+Enter creates a new line.</p></form></div></main>
}

