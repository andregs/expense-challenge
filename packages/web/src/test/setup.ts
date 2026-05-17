import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => {
  cleanup();
});

// jsdom (as of v29) ships HTMLDialogElement but does not implement
// `show`, `showModal` or `close`. Add the minimum behaviour so component
// code that drives the native <dialog> imperatively can be exercised under
// test. Real browsers use their built-in implementations.
// eslint-disable-next-line @typescript-eslint/no-unnecessary-condition -- jsdom lacks showModal at runtime even though the lib types declare it
if (typeof HTMLDialogElement !== 'undefined' && !HTMLDialogElement.prototype.showModal) {
  HTMLDialogElement.prototype.show = function show() {
    this.open = true;
  };
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = function close() {
    this.open = false;
    this.dispatchEvent(new Event('close'));
  };
}
