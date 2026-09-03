export const config = {
  api: {
    bodyParser: false,
  },
};
import { NextResponse } from "next/server";
import Stripe from "stripe";
import { adminDb } from "@/lib/firebaseAdmin"; // adjust if your path is different

export const config = {
  api: {
    bodyParser: false,
  },
};

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
  apiVersion: "2024-06-20",
});

export async function POST(req: Request) {
  try {
    const rawBody = await req.text();
    const signature = req.headers.get("stripe-signature");

    const event = stripe.webhooks.constructEvent(
      rawBody,
      signature!,
      process.env.STRIPE_WEBHOOK_SECRET!
    );

    if (event.type === "checkout.session.completed") {
      const session = event.data.object as any;

      const uid = session.client_reference_id;

      await adminDb
        .collection("users")
        .doc(uid)
        .set({ hasLifetimeAccess: true }, { merge: true });
    }

    return NextResponse.json({ received: true });
  } catch (err: any) {
    console.error("Webhook error:", err);
    return new NextResponse(`Webhook Error: ${err.message}`, { status: 400 });
  }
}
