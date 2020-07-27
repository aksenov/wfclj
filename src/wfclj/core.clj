(ns wfclj.core
  (:require [clojure.inspector]
            [clojure.set]
            [clojure.pprint]))

;;shared logic
(defn valid-dirs [[x y] [max-x max-y]]
  (cond-> []
    (> x 0) (conj [-1 0])
    (< x (dec max-x)) (conj [1 0])
    (> y 0) (conj [0 -1])
    (< y (dec max-y)) (conj [0 1])))

;; derive compatibillities and weights
(def input-matrix-1
  [[:l :l :l :l]
   [:l :l :l :l]
   [:l :l :l :l]
   [:l :l :l :l]
   [:l :c :c :l]
   [:c :s :s :c]
   [:s :s :s :s]
   [:s :s :s :s]])

(defn dimensions [matrix]
  [(count matrix)
   (count (first matrix))])
    
(defn derive-weights [matrix]
  (frequencies
   (apply concat matrix)))

(defn rules [matrix dims]
  (into
   #{}
   (apply concat
          (for [[x row] (map-indexed vector matrix)
                [y tile] (map-indexed vector row)]
            (for [[dx dy :as d] (valid-dirs [x y] dims)]
              (let [other-tile (get-in matrix [(+ x dx) (+ y dy)])]
                [tile d other-tile]))))))

(defn derive-compabilities [matrix]
  (let [dims (dimensions matrix)
        flat-rules (rules matrix dims)]
  (reduce (fn [rule-map [tile d other-tile]]
            (update-in rule-map [tile d] (fnil conj #{other-tile}) other-tile))
          {}
          flat-rules)))

(def compabilities
  (derive-compabilities input-matrix-1))

(def weights
  (derive-weights input-matrix-1))


;; main logic
(def compass {[1 0]  :down
              [-1 0] :up
              [0 -1] :left
              [0 1]  :right})

(defn make-grid [x-dim y-dim tiles]
  (into (sorted-map)
        (for [x (range x-dim)
              y (range y-dim)]
          [[x y] [tiles (valid-dirs [x y] [x-dim y-dim])]])))

(defn print-grid [grid cols]
  (clojure.pprint/pprint
   (for [row (partition cols grid)]
     (map (fn [[_ [l _]]] (first l)) row))))

(defn collapsed-val [loc grid]
  (ffirst (get grid loc)))

(defn neighbour [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn collapsed? [loc grid]
  (not (second (first (get grid loc)))))

(defn fully-collapsed? [grid]
  (every? true? (map collapsed? (keys grid) (repeat grid))))

(defn constrain [tiles1 tiles2]
  (clojure.set/intersection tiles1 tiles2))

(defn tiles-loc [loc grid]
  (first (get grid loc)))  

(defn swap-possible-tiles [tiles loc grid]
  (assoc-in grid [loc 0] tiles))

(defn constrain-dir [loc dir grid]
  (let [val (collapsed-val loc grid)
        loc-neighbour (neighbour loc dir)
        possible-tiles (tiles-loc loc-neighbour)
        allowed-tiles (get-in compabilities [val dir])
        constrained-tiles (constrain possible-tiles allowed-tiles)]
    (swap-possible-tiles constrained-tiles loc-neighbour grid)))

;; https://stackoverflow.com/questions/14464011/idiomatic-clojure-for-picking-between-random-weighted-choices
(defn weighted-rand-choice [m]
    (let [w (reductions #(+ % %2) (vals m))
          r (rand-int (last w))]
         (nth (keys m) (count (take-while #( <= % r ) w)))))
                       
(defn collapse-loc [loc grid]
  (let [choice (weighted-rand-choice
                (select-keys weights (seq (tiles-loc loc grid))))]
    (swap-possible-tiles #{choice} loc grid)))

(defn shannon-entropy [loc grid]
  (let [weights (vals (select-keys weights (seq (tiles-loc loc grid))))
        [sw swlw] (reduce (fn [[sw swlw] w]
                              [(+ sw w) (+ swlw (* w (Math/log w)))])
                            [0 0]
                            weights)]
    (- (Math/log sw) (/ swlw sw))))

(defn kv-for-min-val [m]
  "Returns the [key value] for the minimum v found in m."
  (reduce (fn [[k1 v1]
              [k2 v2]]
            (if (< v2 v1)
              [k2 v2]
              [k1 v1]))
          [nil ##Inf] m))

(defn min-entropy-loc [grid]
  (kv-for-min-val
   (let [ks (remove #(collapsed? % grid) (keys grid))] ;; filter out collapsed locations
     (map (fn [loc g] [loc
                      (- (/ (rand) 1000)
                      (shannon-entropy loc g))])
          ks (repeat grid)))))

(defn grid-iterator [grid]
  ;; TODO implement iterator
  (let [[mel _] (min-entropy-loc grid)
        new-grid (collapse-loc mel grid)]
  (reduce (fn [grid loc] (collapse-loc loc grid)) grid (keys grid))))

(defn iterate-grid [grid]
  (iterate grid-iterator grid))

(defn run [grid]
  (some (fn [g]
          (when (fully-collapsed? g)
            g))
        (iterate-grid grid)))


(shannon-entropy [0 0] (make-grid 8 9 #{:s :l :c}))

(run (make-grid 8 9 #{:s :l :c}))q

(let [grid (make-grid 8 9 #{:s :l :c})]
  (reduce (fn [grid loc] (collapse-loc loc grid)) grid (keys grid)))
(min-entropy-loc (make-grid 8 9 #{:s :l :c}))

(defn update-values [m f & args]
  (into {} (for [[k v] m] [k (apply f v args)])))
