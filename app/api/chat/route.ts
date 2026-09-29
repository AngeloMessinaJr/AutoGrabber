import { NextRequest, NextResponse } from "next/server"
import { adminAuth, adminDb } from "@/lib/firebase-admin"

export const dynamic = "force-dynamic"

async function getUser(request: NextRequest) {
  const authorization = request.headers.get("authorization")
  if (!authorization?.startsWith("Bearer ")) return null
  try {
    return await adminAuth.verifyIdToken(authorization.slice(7))
  } catch {
    return null
  }
}

export async function GET(request: NextRequest) {
  const user = await getUser(request)
  if (!user) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
  const snapshot = await adminDb.collection("chatMessages").orderBy("createdAt", "asc").limitToLast(100).get()
  return NextResponse.json({ messages: snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data(), createdAt: doc.data().createdAt?.toMillis?.() ?? null })) })
}

export async function POST(request: NextRequest) {
  const user = await getUser(request)
  if (!user) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
  const body = await request.json().catch(() => null)
  const text = typeof body?.text === "string" ? body.text.trim() : ""
  if (!text || text.length > 1000) return NextResponse.json({ error: "Message must be between 1 and 1,000 characters." }, { status: 400 })
  const displayName = user.name || user.email?.split("@")[0] || "Member"
  const message = { uid: user.uid, name: displayName, text, createdAt: new Date() }
  const reference = await adminDb.collection("chatMessages").add(message)
  return NextResponse.json({ message: { id: reference.id, ...message, createdAt: message.createdAt.getTime() } }, { status: 201 })
}
