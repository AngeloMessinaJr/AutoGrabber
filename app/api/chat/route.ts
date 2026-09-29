import { NextRequest, NextResponse } from "next/server"
import { adminAuth, adminDb } from "@/lib/firebase-admin"

export const dynamic = "force-dynamic"

async function getUser(request: NextRequest) {
  const authorization = request.headers.get("authorization")
  if (!authorization?.startsWith("Bearer ")) return null
  try { return await adminAuth.verifyIdToken(authorization.slice(7)) } catch { return null }
}

async function getProfile(uid: string, authUser: { name?: string; email?: string | null }) {
  const profile = (await adminDb.collection("users").doc(uid).get()).data() ?? {}
  return {
    name: profile.fullName || authUser.name || authUser.email?.split("@")[0] || "Member",
    avatarUrl: profile.profilePictureUrl || null,
  }
}

export async function GET(request: NextRequest) {
  const user = await getUser(request)
  if (!user) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
  const [messageSnapshot, typingSnapshot] = await Promise.all([
    adminDb.collection("chatMessages").orderBy("createdAt", "asc").limitToLast(100).get(),
    adminDb.collection("chatTyping").where("updatedAt", ">", new Date(Date.now() - 7000)).get(),
  ])
  const typing = await Promise.all(typingSnapshot.docs.filter((doc) => doc.id !== user.uid).map(async (doc) => {
    const data = doc.data(); const profile = await getProfile(doc.id, {})
    return { uid: doc.id, name: profile.name, avatarUrl: profile.avatarUrl }
  }))
  return NextResponse.json({
    messages: messageSnapshot.docs.map((doc) => ({ id: doc.id, ...doc.data(), createdAt: doc.data().createdAt?.toMillis?.() ?? null })),
    typing,
  })
}

export async function POST(request: NextRequest) {
  const user = await getUser(request)
  if (!user) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
  const body = await request.json().catch(() => null)
  if (body?.typing === true) {
    await adminDb.collection("chatTyping").doc(user.uid).set({ updatedAt: new Date() })
    return NextResponse.json({ ok: true })
  }
  if (body?.typing === false) {
    await adminDb.collection("chatTyping").doc(user.uid).delete()
    return NextResponse.json({ ok: true })
  }
  const text = typeof body?.text === "string" ? body.text.trim() : ""
  if (!text || text.length > 1000) return NextResponse.json({ error: "Message must be between 1 and 1,000 characters." }, { status: 400 })
  const profile = await getProfile(user.uid, user)
  const replyTo = typeof body?.replyTo?.id === "string" ? { id: body.replyTo.id, name: String(body.replyTo.name || "Member").slice(0, 100), text: String(body.replyTo.text || "").slice(0, 200) } : null
  const message = { uid: user.uid, name: profile.name, avatarUrl: profile.avatarUrl, text, replyTo, createdAt: new Date() }
  const reference = await adminDb.collection("chatMessages").add(message)
  await adminDb.collection("chatTyping").doc(user.uid).delete()
  return NextResponse.json({ message: { id: reference.id, ...message, createdAt: message.createdAt.getTime() } }, { status: 201 })
}

export async function DELETE(request: NextRequest) {
  const user = await getUser(request)
  if (!user) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
  await adminDb.collection("chatTyping").doc(user.uid).delete()
  return NextResponse.json({ ok: true })
}
