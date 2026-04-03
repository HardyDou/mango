/**
 * Canvas mock setup for Vitest with happy-dom
 * This file should be imported in vitest.config.ts as setupFiles
 */

// Mock canvas 2d context for happy-dom
class MockCanvasRenderingContext2D {
  strokeStyle = '';
  lineWidth = 1;
  lineCap = 'round';
  lineJoin = 'round';
  globalAlpha = 1;
  globalCompositeOperation = 'source-over';
  canvas: HTMLCanvasElement | null = null;

  beginPath = vi.fn();
  moveTo = vi.fn();
  lineTo = vi.fn();
  stroke = vi.fn();
  fill = vi.fn();
  clearRect = vi.fn();
  drawImage = vi.fn();
  getImageData = vi.fn(() => ({ data: [] as number[] }));
  putImageData = vi.fn();
  createImageData = vi.fn(() => ({ data: [] as number[] }));
  fillRect = vi.fn();
  strokeRect = vi.fn();
  arc = vi.fn();
  fillText = vi.fn();
  measureText = vi.fn(() => ({ width: 0 }));
  save = vi.fn();
  restore = vi.fn();
  scale = vi.fn();
  rotate = vi.fn();
  translate = vi.fn();
  transform = vi.fn();
  setTransform = vi.fn();
  drawFocusIfNeeded = vi.fn();
  scrollPathIntoView = vi.fn();
  getBoundingClientRect = vi.fn(() => ({ x: 0, y: 0, width: 0, height: 0, top: 0, left: 0, bottom: 0, right: 0 }));
  createLinearGradient = vi.fn(() => ({
    addColorStop: vi.fn(),
  }));
  createRadialGradient = vi.fn(() => ({
    addColorStop: vi.fn(),
  }));
}

// Ensure HTMLCanvasElement exists and add getContext
if (typeof HTMLCanvasElement !== 'undefined') {
  HTMLCanvasElement.prototype.getContext = vi.fn((contextId: string): CanvasRenderingContext2D | null => {
    if (contextId === '2d') {
      return new MockCanvasRenderingContext2D() as unknown as CanvasRenderingContext2D;
    }
    return null;
  });

  // Mock toDataURL and toBlob
  HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,');
  HTMLCanvasElement.prototype.toBlob = vi.fn((callback: BlobCallback | null) => {
    if (callback) {
      callback(new Blob([''], { type: 'image/png' }));
    }
  });
}

// Mock Image class
class MockImage {
  src = '';
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
  width = 0;
  height = 0;
  complete = false;
}

if (typeof window !== 'undefined') {
  (window as any).Image = MockImage;
}
