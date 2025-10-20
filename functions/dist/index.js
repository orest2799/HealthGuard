"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.annotateImage = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const vision_1 = __importDefault(require("@google-cloud/vision"));
admin.initializeApp();
const visionClient = new vision_1.default.ImageAnnotatorClient();
exports.annotateImage = functions
    .region("europe-west4")
    .https.onRequest(async (req, res) => {
    try {
        if (req.method !== "POST") {
            res.status(405).send("Use POST");
            return;
        }
        const imageBase64 = req.body?.imageBase64;
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
    }
    catch (e) {
        console.error(e);
        res.status(500).json({ error: e?.message ?? "OCR failed" });
    }
});
