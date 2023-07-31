;; Borrowed from AssemblyScript's fibonacci example
;; INFO asc module.ts --textFile module.wat --outFile module.wasm --bindings raw -O3 --runtime stub
(module
 (type $i32_=>_i32 (func (param i32) (result i32)))
 (memory $0 0)
 (export "fib" (func $module/fib))
 (export "memory" (memory $0))
 (func $module/fib (param $0 i32) (result i32)
  (local $1 i32)
  (local $2 i32)
  (local $3 i32)
  i32.const 1  ;; 1
  local.set $1 ;; 0
  local.get $0 ;; 1
  i32.const 0  ;; 2
  i32.gt_s     ;; 1
  if           ;; 0
   loop $while-continue|0
    local.get $0  ;; 1
    i32.const 1   ;; 2
    i32.sub       ;; 1
    local.tee $0  ;; 1
    if            ;; 0
     local.get $1 ;; 1
     local.get $2 ;; 2
     i32.add      ;; 1
     local.set $3 ;; 0
     local.get $1 ;; 1
     local.set $2 ;; 0
     local.get $3 ;; 1
     local.set $1 ;; 0
     br $while-continue|0
    end
   end
   local.get $1   ;; 1
   return         ;; return
  end
  i32.const 0     ;; 1
 )
)