import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCall } from "firebase-functions/v2/https";
import { ImageAnnotatorClient } from "@google-cloud/vision";

// Initialize Firebase Admin
admin.initializeApp();

// Initialize Google Vision Client
const visionClient = new ImageAnnotatorClient();

// Define types - removed unused OcrRequest
interface OcrResponse {
  text: string;
  confidence?: number;
  language?: string;
  error?: string;
}

// Main OCR function
export const extractTextWithGoogleVision = onCall(
  async (req): Promise<OcrResponse> => {
    try {
      // Authentication check
      if (!req.auth) {
        throw new functions.https.HttpsError(
          "unauthenticated",
          "User must be authenticated"
        );
      }

      const { imageData, languages = ['el', 'en'] } = req.data;

      if (!imageData) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Image data is required"
        );
      }

      // Process image with Google Vision OCR
      const result = await processImageWithGoogleVision(imageData, languages);

      return result;
    } catch (error) {
      console.error("Google Vision OCR Error:", error);

      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";

      return {
        text: "",
        error: errorMessage
      };
    }
  }
);

// Enhanced OCR function with better error handling
export const advancedTextExtraction = onCall(
  async (req): Promise<OcrResponse> => {
    try {
      if (!req.auth) {
        throw new functions.https.HttpsError(
          "unauthenticated",
          "User must be authenticated"
        );
      }

      const { imageData, languages = ['el', 'en'] } = req.data;

      if (!imageData) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Image data is required"
        );
      }

      // Validate languages
      const validLanguages = validateLanguages(languages);
      if (validLanguages.length === 0) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "No valid languages provided. Supported: el, en"
        );
      }

      // Process with Google Vision
      const result = await processWithEnhancedOCR(imageData, validLanguages);

      return result;

    } catch (error) {
      console.error("Advanced OCR Error:", error);

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      const errorMessage = error instanceof Error ? error.message : "Internal server error";

      return {
        text: "",
        error: errorMessage
      };
    }
  }
);

// Google Vision processing function
async function processImageWithGoogleVision(
  imageData: string,
  languages: string[]
): Promise<OcrResponse> {
  try {
    // Prepare image buffer (remove data URL prefix if present)
    let cleanImageData = imageData;
    if (imageData.startsWith('data:image/')) {
      cleanImageData = imageData.split(',')[1];
    }

    const imageBuffer = Buffer.from(cleanImageData, 'base64');

    // Prepare request with language hints
    const request = {
      image: { content: imageBuffer },
      imageContext: {
        languageHints: languages, // Greek ('el') and English ('en')
      },
    };

    // Perform text detection
    const [result] = await visionClient.textDetection(request);
    const detections = result.textAnnotations;

    if (!detections || detections.length === 0) {
      return {
        text: "",
        confidence: 0,
        language: "unknown"
      };
    }

    // Get full text (first annotation is the entire text)
    const fullText = detections[0].description || "";

    // Calculate average confidence
    const confidences = detections
      .filter((det: any) => det.confidence !== undefined && det.confidence !== null)
      .map((det: any) => det.confidence as number);

    const averageConfidence = confidences.length > 0
      ? confidences.reduce((sum: number, conf: number) => sum + conf, 0) / confidences.length
      : 0;

    return {
      text: fullText,
      confidence: averageConfidence,
      language: detectPrimaryLanguage(fullText)
    };

  } catch (error) {
    console.error("Google Vision processing error:", error);
    const errorMessage = error instanceof Error ? error.message : "Google Vision processing failed";
    throw new Error(errorMessage);
  }
}

// Enhanced OCR processing
async function processWithEnhancedOCR(
  imageData: string,
  languages: string[]
): Promise<OcrResponse> {
  try {
    // Clean image data
    const cleanImageData = imageData.replace(/^data:image\/[a-z]+;base64,/, '');
    const imageBuffer = Buffer.from(cleanImageData, 'base64');

    // Enhanced request with multiple language support
    const request = {
      image: { content: imageBuffer },
      imageContext: {
        languageHints: languages,
        // Additional enhancements for better OCR
        textDetectionParams: {
          enableTextDetectionConfidenceScore: true,
        },
      },
    };

    const [result] = await visionClient.textDetection(request);
    const detections = result.textAnnotations;

    if (!detections || detections.length === 0) {
      return {
        text: "",
        confidence: 0,
        language: "unknown",
        error: "No text detected in image"
      };
    }

    const fullText = detections[0].description || "";
    const confidence = calculateConfidence(detections);
    const detectedLanguage = detectLanguageWithConfidence(fullText);

    return {
      text: fullText.trim(),
      confidence: confidence,
      language: detectedLanguage
    };

  } catch (error) {
    console.error("Enhanced OCR processing error:", error);
    const errorMessage = error instanceof Error ? error.message : "OCR processing failed";
    throw new Error(errorMessage);
  }
}

// Helper functions
function validateLanguages(languages: string[]): string[] {
  const supportedLanguages = ['el', 'en', 'gr', 'eng']; // Greek and English variants
  return languages.filter(lang =>
    supportedLanguages.includes(lang.toLowerCase())
  );
}

function calculateConfidence(detections: any[]): number {
  const confidences = detections
    .filter((det: any) => det.confidence !== undefined && det.confidence !== null)
    .map((det: any) => det.confidence as number);

  return confidences.length > 0
    ? confidences.reduce((sum: number, conf: number) => sum + conf, 0) / confidences.length
    : 0;
}

function detectPrimaryLanguage(text: string): string {
  // Simple language detection based on character frequency
  const greekChars = /[α-ωΑ-Ω]/g;
  const englishChars = /[a-zA-Z]/g;

  const greekCount = (text.match(greekChars) || []).length;
  const englishCount = (text.match(englishChars) || []).length;

  if (greekCount > englishCount) return 'el'; // Greek
  if (englishCount > greekCount) return 'en'; // English
  return 'mixed'; // Mixed or undetermined
}

function detectLanguageWithConfidence(text: string): string {
  if (text.length < 10) return 'unknown';

  const greekRegex = /[α-ωΑ-Ω]/g;
  const englishRegex = /[a-zA-Z]/g;

  const greekMatches = text.match(greekRegex) || [];
  const englishMatches = text.match(englishRegex) || [];

  const totalLetters = greekMatches.length + englishMatches.length;

  if (totalLetters === 0) return 'unknown';

  const greekRatio = greekMatches.length / totalLetters;
  const englishRatio = englishMatches.length / totalLetters;

  if (greekRatio > 0.7) return 'el';
  if (englishRatio > 0.7) return 'en';
  return 'mixed';
}