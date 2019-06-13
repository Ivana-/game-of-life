(ns game-of-life.model
  (:require
   [re-frame.core :as re-frame]))

(def state-path [:state])

(re-frame/reg-sub
 ::state
 (fn [db] (get-in db state-path)))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   (assoc-in {} state-path {:rows 10
                            :cols 10
                            :movie-frame-duration-ms 500})))

(re-frame/reg-event-db
 ::new-game
 (fn [db _] (update-in db state-path select-keys [:rows :cols :movie-frame-duration-ms])))

(re-frame/reg-event-fx
 ::invert-cell
 (fn [{db :db} [_ r c]]
   (when-not (> (or (get-in db (conj state-path :step)) 0) 0)
     {:db (update-in db (conj state-path :field [r c]) not)})))

(re-frame/reg-event-db
 ::set-val
 (fn [db [_ path val]]
   (assoc-in db (into state-path path) val)))

;; steps forward / back

(defn step-forward [db]
  (let [{:keys [rows cols field step history]} (get-in db state-path)
        alive-cell? (fn [r c] (get field [(mod r rows) (mod c cols)]))
        alive-neighbors-count (fn [r c]
                                (->> (for [dr [-1,0,1] dc [-1,0,1]
                                           :when (not (= 0 dr dc))]
                                       (if (alive-cell? (+ r dr) (+ c dc)) 1 0))
                                     (reduce + 0)))
        new-field (->> (for [r (range rows) c (range cols)]
                         (let [an (alive-neighbors-count r c)
                               ac (if (alive-cell? r c) (<= 2 an 3) (= 3 an))]
                           (if ac [[r c] true])))
                       (filter identity)
                       (into {}))]
    (update-in db state-path merge {:field new-field
                                    :step (inc (or step 0))
                                    :history (take 50 (cons field history))})))

(re-frame/reg-event-db
 ::step-forward
 (fn [db _] (step-forward db)))

(re-frame/reg-event-fx
 ::step-back
 (fn [{db :db} _]
   (let [{:keys [step history]} (get-in db state-path)]
     (when-not (empty? history)
       {:db (update-in db state-path merge {:field (first history)
                                            :step (dec step)
                                            :history (rest history)})}))))

(re-frame/reg-event-db
 ::jump-to-step
 (fn [db _]
   (let [{:keys [step step-dest history]} (get-in db state-path)
         step-start (or step 0)
         error (cond
                 (nil? step-dest) "Set destination step"
                 
                 (< step-dest step-start)
                 (let [d (count history)]
                   (when (> (- step-start step-dest) d)
                     (if (zero? d)
                       "We can not jump any step back"
                       (str "We can jump max " d " steps back")))))]
     (if error
       (update-in db state-path assoc :jump-error error)
       (let [db* (cond
                   (= step-dest step-start) db

                   (< step-start step-dest) ;; jump forward
                   (loop [d db]
                     (let [{step :step} (get-in d state-path)]
                       (if (< (or step 0) step-dest) (recur (step-forward d)) d)))

                   :else ;; jump back
                   (let [h (drop (dec (- step-start step-dest)) history)]
                     (update-in db state-path merge {:field (first h)
                                                     :step step-dest
                                                     :history (rest h)})))]
         (update-in db* state-path dissoc :step-dest :jump-error))))))

;; timer/movie

(re-frame/reg-sub
 ::sub-path
 (fn [db [_ path]]
   (get-in db (into state-path path))))

(defn on-tik [state]
  (let [movie-on? @(re-frame/subscribe [::sub-path [:movie-on?]])]
    (when movie-on? (re-frame/dispatch [::step-forward]))
    ;; (if movie-on? (prn :on-tik :move) (prn :on-tik))
    movie-on?))

(defn periodic [f v]
  (let [timeout-ms @(re-frame/subscribe [::sub-path [:movie-frame-duration-ms]])]
    (-> (js/Promise. (fn [resolve] (js/setTimeout #(resolve (f v)) timeout-ms)))
        (.then #(if % (periodic f v)))
        (.catch prn))))

(re-frame/reg-event-db
 ::movie
 (fn [db _]
   (if-not (get-in db (conj state-path :movie-on?)) (periodic on-tik db))
   (update-in db (conj state-path :movie-on?) not)))
