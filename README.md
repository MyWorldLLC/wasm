# Experimental WASM Runtime

This is an **experimental** WASM runtime for the JVM. It does not yet implement the full Wasm 1.0 spec,
and it does not yet implement Wasm 2.0 or common proposals in any capacity. Some Wasm specifications around
trapping for invalid instruction parameters are relaxed.

Features:
 * JVM bytecode compilation (no interpreter mode)
 * Host <-> Wasm interop via MethodHandle
 * Off-heap (native) memory allocation (via Panama's Foreign Memory API)
 * Memory allocation in sub-page sizes (optional)
