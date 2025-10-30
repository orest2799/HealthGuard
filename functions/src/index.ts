// src/index.ts
import functions from "@google-cloud/functions-framework";
import { ImageAnnotatorClient } from "@google-cloud/vision";

const client = new ImageAnnotatorClient();

/**
 * HTTP POST body:
 *  { imageUri?: string, contentBase64?: string, features?: { type: string, maxResults?: number }[] }
 */
functions.http("annotateImage", async (req, res) => {
  try {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Use POST" });
      return;
    }

    const { imageUri, contentBase64, features } = req.body || {};

    if (!imageUri && !contentBase64) {
      res.status(400).json({ error: "Provide imageUri or contentBase64" });
      return;
    }

    const request = {
      image: imageUri ? { source: { imageUri } } : { content: contentBase64 },
      features:
        Array.isArray(features) && features.length
          ? features
          : [{ type: "LABEL_DETECTION", maxResults: 10 }],
    };

    const [resp] = await client.annotateImage(request as any);
    res.json(resp);
  } catch (e: any) {
    console.error(e);
    res.status(500).json({ error: e?.message || "Internal error" });
  }
});

