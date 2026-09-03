import { NextResponse } from "next/server"
import type Stripe from "stripe"
import { getAdminDb } from "@/lib/firebase-admin"
import { stripe } from "@/lib/stripe"

export const runtime = "nodejs"

export async function POST(request: Request) {
  const signature = request.headers.get("stripe-signature")
  const secret = process.env.STRIPE_WEBHOOK_SECRET
  if (!signature || !secret) {
    return NextResponse.json({ error: "Missing Stripe webhook configuration." }, { status: 400 })
  }

  try {
    const event = stripe.webhooks.constructEvent(await request.text(), signature, secret)
    if (event.type === "checkout.session.completed" || event.type === "checkout.session.async_payment_succeeded") {
      const session = event.data.object as Stripe.Checkout.Session
      const userId = session.client_reference_id || session.metadata?.userId

      if (!userId) {
        console.error("[v0] Stripe checkout session is missing a Firebase user ID", { sessionId: session.id })
      } else if (session.payment_status === "paid") {
        await getAdminDb().collection("users").doc(userId).set(
          {
            hasLifetimeAccess: true,
            status: "active",
            purchasedAt: new Date(),
          },
          { merge: true },
        )
      }
    }
    return NextResponse.json({ received: true })
  } catch (error) {
    console.error("[v0] Stripe webhook processing failed", error)
    return NextResponse.json({ error: "Invalid webhook." }, { status: 400 })
  }
}
