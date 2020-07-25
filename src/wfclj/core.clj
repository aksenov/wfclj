(ns wfclj.core)

(defn make-grid [x-dim y-dim tiles]
  (into {}
        (for [x (range x-dim)
              y (range y-dim)]
          [[x y] tiles])))


(defn valid-dirs [[x y] [max-x max-y]]
  (map first
       (remove (fn [[_ x y]] (or (neg? x) (neg? y) (>= x max-x) (>= y max-y)))
          (for [dx [-1 0 1] dy (if (zero? dx) [-1 1] [-1 0 1])]
            [[dx dy] (+ dx x) (+ dy y)]))))

(defn valid-dirs [[x y] [max-x max-y]]
  (cond-> []
    (> x 0) (conj [-1 0])
    (< x (dec max-x)) (conj [1 0])
    (> y 0) (conj [0 -1])
    (< y (dec max-y)) (conj [0 1])))

(defn dimensions [matrix]
  [(count matrix)
   (count (first matrix))])

(def input-matrix-1
  [[:l :l :l :l]
   [:l :l :l :l]
   [:l :l :l :l]
   [:l :l :l :l]
   [:l :c :c :l]
   [:c :s :s :c]
   [:s :s :s :s]
   [:s :s :s :s]])
    
(defn weights [matrix]
  (frequencies
   (apply concat matrix)))

(defn neighbour [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn coords [matrix]
  (for [x-dim (range (count matrix))
        y-dim (range (count (first matrix)))]
    [x-dim y-dim]))

(defn neighbours-vals [matrix neighbours]
  (map #(get-in matrix %) neighbours))

(defn magic [vs dirs ns]
  (map (fn [v d n] (vector v d n)) vs dirs ns))

(defn make-comp-mapper [matrix]
  (let [dims (dimensions matrix)]
  (fn [coords]
    (let [value (get-in matrix coords)
          dirs (valid-dirs coords dims)
          neighbours (map neighbour (repeat coords) dirs)
          neighbour-vals (neighbours-vals matrix neighbours)]
      (magic (repeat value) dirs neighbour-vals)))))

(let [matrix input-matrix-1]
  (set
   (mapcat (make-comp-mapper matrix) (coords matrix))))              
