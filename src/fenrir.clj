;; Copyright (C) 2011, Eduardo Julián. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the 
;; Eclipse Public License 1.0
;; (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;; in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns fenrir
  "An alternative class-based object-system for Clojure based on hash-maps and multimethods."
  (:use clojure.set))

(declare fenrir-class-dispatch fenrir-dispatch)

; Utility fns
(defn- add-ns "Namespace qualifies a symbol."
  [sym]
  (let [str-sym (str sym)]
    (if (.contains str-sym "/")
      (let [ns-str (first (.split str-sym "/"))]
        (if (not (= ns-str (str *ns*)))
          (symbol (str *ns* "/" (second (.split str-sym "/"))))
          sym))
      (symbol (str *ns* "/" str-sym)))))

(defn- multi? [m] (try (if (instance? clojure.lang.MultiFn (eval m)) true false) (catch Exception e false)))
(defn- any? [f coll] (if (some f coll) true false))

(defn- recreplace "A recursive version of the replace fn."
  [repmap coll]
  (if-not (or (seq? coll) (vector? coll))
    (first (recreplace repmap [coll]))
    (if-not (some coll? coll)
      (replace repmap coll)
      (let [seqfn (cond (seq? coll) seq (vector? coll) vec)]
        (seqfn (for [nc (replace repmap coll)] (if-not (coll? nc) nc (recreplace repmap nc))))))))

; Fenrir base data
(def #^{:doc "The type of all Fenrir fclasses."}
  fClass
  (with-meta {:name `fClass, :slots '[slots], :base-map {:slots []}} {:type `fClass}))

(def #^{:doc "The base fobject class."}
  fObject
  (with-meta {:name `fObject, :slots [], :base-map {}} {:type `fClass}))

(defn- make-class "Creates an fclass meta-object."
  [name slots virtual-slots base-map abstract?]
  (assoc fClass :name name, :slots slots, :virtual-slots virtual-slots, :base-map base-map, :abstract? abstract?))

(defn- get-super-slots "Returns the slots from the given superclasses and their own superclasses."
  [supers]
  (let [dads (for [s supers] (:slots (eval s)))
        grandpas (->> supers (map ancestors) (map seq) flatten distinct)
        grandpas (for [s grandpas] (:slots (eval s)))]
    (apply concat (concat dads grandpas))))

(defn- slots-by-base
  [bases]
  (let [all-bases (concat bases (->> bases (map ancestors) (map seq) flatten distinct))
        class-slots (map (fn [_] [_ (apply hash-set (:slots (eval _)))]) all-bases)]
    class-slots))

(defn- get-super-vslots "Returns the virtual-slots from the given superclasses and their own superclasses."
  [supers]
  (let [dads (for [s supers] (:virtual-slots (eval s)))
        grandpas (->> supers (map ancestors) (map seq) flatten distinct)
        grandpas (for [s grandpas] (:slots (eval s)))]
    (apply concat (concat dads grandpas))))

(defn- vslots-by-base
  [bases]
  (let [all-bases (concat bases (->> bases (map ancestors) (map seq) flatten distinct))
        class-slots (map (fn [_] [_ (apply hash-set (:virtual-slots (eval _)))]) all-bases)]
    class-slots))

(defn- in-seq? [x coll] (any? #(= x %) coll))

; Class definition functionality
(defmacro defclass
  "Creates an fClass meta-object for creating fobjects."
  [sym supers slots & meths]
  (let [; Extract the doc-string
        doc-str (when (string? (first meths)) (first meths))
        meths (if (string? (first meths)) (rest meths) meths)
        ; Complete the set of slots for the meta-object.
        super-slots (get-super-slots supers)
        _ (let [freqs (remove nil? (for [s (frequencies super-slots)] (when (> (second s) 1) (first s))))
                freqs (filter #(not (in-seq? % slots)) freqs)]
            (when-not (empty? freqs) (throw (Exception. (str "Repeated slots " (seq freqs) " are being inherited.")))))
        _slots (filter #(not (in-seq? % super-slots)) slots)
        all-slots (vec (distinct (concat _slots super-slots)))
        inherited-slots (slots-by-base supers)
        ; Classify methods, accessors/getters and mutators/setters.
        gets (filter #(= 'get (first %)) meths)
        sets (filter #(= 'set (first %)) meths)
        ; Virtual slots
        super-vslots (get-super-vslots supers)
        vslots (vec (remove nil? (for [g gets] (if (in-seq? (second g) all-slots) nil (second g)))))
        _ (let [freqs (remove nil? (for [s (frequencies super-vslots)] (when (> (second s) 1) (first s))))
                freqs (filter #(not (in-seq? % vslots)) freqs)]
            (when-not (empty? freqs) (throw (Exception. (str "Repeated virtual-slots " (seq freqs) " are being inherited.")))))
        all-vslots (vec (distinct (concat vslots super-vslots)))
        inherited-vslots (vslots-by-base supers)
        ; Masks
        masks (filter #(= 'mask (first %)) meths)
        slot-masks (filter #(or (in-seq? (second %) all-slots)
                                (in-seq? (second %) all-vslots)) masks)
        meth-masks (filter #(not (or (in-seq? (second %) all-slots)
                                     (in-seq? (second %) all-vslots))) masks)
        meths (filter #(not (or (= 'get (first %)) (= 'set (first %)) (= 'mask (first %)))) meths)
        ; Create the slot substitutions for usage inside method bodies.
        susts (apply hash-map (interleave all-slots (for [s all-slots] `(~'*self* ~(keyword s)))))
        ; Create all the meta-object data.
        class-sym (add-ns sym)
        base-map (apply hash-map (interleave (map keyword all-slots) (repeat nil)))
        abstract? (any? #(and (= 2 (count %))
                              (not (or (= 'get (first %))
                                       (= 'set (first %)))))
                        meths)
        meta-obj (make-class class-sym slots vslots base-map abstract?)
        ; Create documentation
        fdoc (str "Fenrir Class Name: " class-sym "\n"
                  "Abstract? " abstract? "\n"
                  "Slots: " slots "\n"
                  "Virtual Slots: " vslots)
        doc-str (if doc-str (str fdoc "\n\n" doc-str) fdoc)
        ]
    `(do
       ; Implement type derivations.
       ~@(if (empty? supers)
           [`(derive '~class-sym `fObject)]
           (for [s supers] `(derive '~class-sym '~s)))
       (def ~(with-meta sym {:doc doc-str}) '~meta-obj)
       ; Implement methods.
       ~@(for [m meths]
           (cond
             ; Abstract fns.
             (< (count m) 3) `(do (defmulti ~@(identity m) fenrir-dispatch)
                                (defmethod ~(first m) '~class-sym [o# & args#]
                                  (throw (Exception. ~(str "Unimplemented method in class " ~class-sym ".")))))
             ; Multimethod implementation.
             :else (if (multi? (first m))
                     `(defmethod ~(first m) '~class-sym ~(vec (cons '*self* (second m))) ~@(recreplace susts (nthnext m 2)))
                     (let [[m-sym & others] m
                           doc-str (if (string? (first others)) (first others) "")
                           [argsv & forms] (if (string? (first others)) (rest others) others)]
                       `(do (defmulti ~m-sym ~doc-str fenrir-dispatch)
                          (defmethod ~m-sym '~class-sym ~(vec (cons '*self* argsv)) ~@(recreplace susts forms))))
                     )))
       ~@(for [m meth-masks]
           `(defmethod ~(second m) '~class-sym [o# args#] (apply call-base ~(second m) '~(nth m 2) o# args#)))
       ; Implement accessors/getters
       (defmethod get-slot '~class-sym [~'*self* ~'*slot-key*]
         (case ~'*slot-key*
           ~@(reduce concat (for [g gets] [(keyword (second g))
                                           (if (first (nnext g)) (recreplace susts (nth g 2)) nil)]))
           ; Slot masks & virtual slots.
           ~@(reduce concat (for [m slot-masks] [(keyword (second m))
                                                 `(call-base get-slot '~(nth m 2) ~'*self* ~'*slot-key*)]))
           ; Inherited slot redirection.
           ~@(reduce concat (for [is inherited-slots,
                                  s (difference (second is) (apply hash-set slots))]
                              [(keyword s) `(call-base get-slot '~(first is) ~'*self* ~'*slot-key*)]))
           ; Inherited virtual slots.
           ~@(reduce concat (for [iv inherited-vslots
                                  s (difference (second iv) (apply hash-set vslots))]
                              [(keyword s) `(call-base get-slot '~(first iv) ~'*self* ~'*slot-key*)]))
           ; Default case.
           (~'*self* ~'*slot-key*)))
       ; Implement mutators/setters
       (defmethod set-slot '~class-sym
         ([~'*self* ~'*slot-key* ~'*val*]
          (case ~'*slot-key*
            ; Mutators/setters.
            ~@(reduce concat (for [s sets] [(keyword (second s))
                                            `(assoc ~'*self* ~(keyword (second s)) ~(if (first (nnext s)) (recreplace susts (nth s 2)) nil))]))
            ; Slot masks.
            ~@(reduce concat (for [m slot-masks] [(keyword (second m))
                                                  `(call-base get-slot '~(nth m 2) ~'*self* ~'*slot-key* ~'*val*)]))
            ; Inherited slot redirection.
            ~@(reduce concat (for [is inherited-slots,
                                  s (difference (second is) (apply hash-set slots))]
                              [(keyword s) `(call-base set-slot '~(first is) ~'*self* ~'*slot-key* ~'*val*)]))
            ; Virtual slots.
            ~@(reduce concat (for [v vslots] [(keyword v) '*self*]))
            ; Inherited virtual slots.
            ~@(reduce concat (for [iv inherited-vslots
                                  s (difference (second iv) (apply hash-set vslots))]
                              [(keyword s) '*self*]))
            ; Default case.
            (assoc ~'*self* ~'*slot-key* ~'*val*)
            ))
         ([~'*self* ~'*slot-key* ~'*val* & kvs#]
          (apply set-slot (set-slot ~'*self* ~'*slot-key* ~'*val*) kvs#)))
       )))

; Object-handling fns
(defn fenrir-class-dispatch [c & _] (:name c))
(defn fenrir-dispatch [o & _] (type o))

(defmulti ctor
  "A fn that works as a constructor for fobjects.
It can be extended to provide custom constructors."
  fenrir-class-dispatch)
(defmethod ctor `fObject [fclass & kvals]
  (if (:abstract? fclass)
    (throw (Exception. "Abstract classes cannot be instantiated."))
    (with-meta
      (if (empty? kvals)
        (:base-map fclass)
        (apply assoc (:base-map fclass) kvals))
      {:type (:name fclass)})))

(defmulti get-slot
  "A fn for getting a slot in an fobject.
It can be extended to create accessors/getters."
  (fn [o & _] (type o)))
(defmethod get-slot `fObject [o k] (o k))

(defmulti set-slot
  "A fn for setting a slot in an fobject.
It can be extended to create mutators/setters."
  (fn [o & _] (type o)))
(defmethod set-slot `fObject
  ([o k v] (assoc o k v))
  ([o k v & kvs] (apply set-slot (assoc o k v) kvs)))

(def #^{:doc "The base (fObject's) ctor."}
  base-ctor (get-method ctor `fObject))

(def #^{:doc "The base (fObject's) accessor/getter."}
  base-get (get-method get-slot `fObject))

(def #^{:doc "The base (fObject's) mutator/setter."}
  base-set (get-method set-slot `fObject))

(defn call-base
  "Invokes a base class' implementation of a method with the given arguments."
  [meth base obj & args]
  (if (isa? (type obj) (:name base))
    (apply (get-method meth (:name base)) obj args)
    (throw (Exception. (str "The given object is not a " (:name base) ".")))))
