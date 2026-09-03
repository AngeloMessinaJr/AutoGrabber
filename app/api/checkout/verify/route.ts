import { NextResponse } from "next/server"
import { getAdminAuth, getAdminDb } from "@/lib/firebase-admin"
import { stripe } from "@/lib/stripe"

export const runtime = "nodejs"

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get("authorization")
    const idToken = authHeader?.startsWith("Bearer ") ? authHeader.slice(7) : null
    if (!idToken) return NextResponse.json({ error: "Authentication required." }, { status: 401 })

    const { uid } = await getAdminAuth().verifyIdToken(idToken)
    const body = await request.json().catch(() => null)
    const sessionId = body?.session_id
    if (typeof sessionId !== "string" || !sessionId.startsWith("cs_")) {
      return NextResponse.json({ error: "Invalid checkout session." }, { status: 400 })
    }

    const session = await stripe.checkout.sessions.retrieve(sessionId)
    const userId = session.client_reference_id || session.metadata?.userId
    if (!userId || userId !== uid) {
      return NextResponse.json({ error: "Checkout session does not belong to this account." }, { status: 403 })
    }
    if (session.payment_status !== "paid") {
      return NextResponse.json({ hasLifetimeAccess: false, status: session.payment_status }, { status: 402 })
    }

    await getAdminDb().collection("users").doc(uid).set(
      { hasLifetimeAccess: true, status: "active", purchasedAt: new Date() },
      { merge: true },
    )
    return NextResponse.json({ hasLifetimeAccess: true })
  } catch (error) {
    console.error("[v0] checkout verification failed", error)
    return NextResponse.json({ error: "Unable to verify checkout payment." }, { status: 500 })
  }
}
