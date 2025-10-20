import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import vision from "@google-cloud/vision";

admin.initializeApp();
const visionClient = new vision.ImageAnnotatorClient();

export const annotateImage = functions
  .region("europe-west4")
  .https.onRequest(async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).send("Use POST");
        return;
      }

      const imageBase64: string | undefined = req.body?.imageBase64;
      if (!imageBase64) {
        res.status(400).json({ error: "imageBase64 is required" });
        return;
      }

      // If the client sent a data URL, strip the prefix
      const clean = imageBase64.replace(/^data:image\/[a-z]+;base64,/, "");
      const buffer = Buffer.from(clean, "base64");

      const [result] = await visionClient.textDetection({ image: { content: buffer } });
      const annotations = result.textAnnotations ?? [];
      const fullText = annotations.length ? annotations[0].description ?? "" : "";

      res.json({
        text: fullText,
        words: annotations.slice(1).map(a => a.description ?? "")
      });
    } catch (e: any) {
      console.error(e);
      res.status(500).json({ error: e?.message ?? "OCR failed" });
    }
  });
