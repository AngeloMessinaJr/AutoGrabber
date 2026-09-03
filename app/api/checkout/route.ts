import { NextResponse } from "next/server"
import { stripe } from "@/lib/stripe"
import { LIFETIME_PRODUCT } from "@/lib/products"
import { adminAuth } from "@/lib/firebase-admin"

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get("authorization")
    const idToken = authHeader?.startsWith("Bearer ") ? authHeader.slice(7) : null
    if (!idToken) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
    const token = await adminAuth.verifyIdToken(idToken)

    const body = await request.json()
    if (body.uid && body.uid !== token.uid) return NextResponse.json({ error: "Invalid request." }, { status: 403 })
    const uid = token.uid

    const origin = request.headers.get("origin") || new URL(request.url).origin
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: [{ price_data: { currency: "usd", product_data: { name: LIFETIME_PRODUCT.name, description: LIFETIME_PRODUCT.description }, unit_amount: LIFETIME_PRODUCT.priceInCents }, quantity: 1 }],
      success_url: `${origin}/account?purchase=success`,
      cancel_url: `${origin}/account?purchase=canceled`,
      client_reference_id: uid,
      metadata: { firebaseUid: uid },
      integration_identifier: `autograbber_lifetime_${Math.random().toString(36).slice(2, 10)}`,
    })
    return NextResponse.json({ url: session.url })
  } catch (error) {
    console.error("[v0] checkout session creation failed", error)
    return NextResponse.json(
      { error: "Unable to start checkout. Please try again." },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    )
  }
}
