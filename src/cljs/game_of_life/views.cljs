(ns game-of-life.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [garden.core :as garden]
   [game-of-life.model :as model]))

(defn style [{:keys [transition-time]}]
  (let [transition (str "background-color " transition-time "s ease-in")]
    [:style
     (garden/css
      [:html {:height "100%" :margin 0}
       [:body {:height "100%" :margin 0}
        [:.app {:height "100%" :margin 0}
         [:.page {:display :flex
                  ; :height "100vh" "100%"
                  :position :absolute
                  :top "8px" :right "8px" :bottom "8px" :left "8px"
                  :margin 0 :padding 0}

          ;; bar
          [:.bar {:display :flex
                  :flex-direction :column
                  :background-color :antiquewhite
                  :margin-right "8px"
                  :padding "10px"}
           [:.header {:font-size "20pt"
                      :margin "0 auto"}]
           [:.splitter {:height "25px"}]
           [:.label {:padding-right "10px"
                     :margin-top "5px"}]
           [:.error {:color :red
                     :margin-top "15px"}]
           [:.input :button {:outline :none
                             :margin-top "5px"
                             :font-size "12pt"
                             :flex-grow 1
                             ;; :flex-shrink 1
                             :width "50px"}]
           [:button {:min-height "27px"}]
           [:.range {:display :block
                     :flex-grow 1}]
           [:.bar-row {:display :flex
                       :align-items :baseline}]
           [:.bar-row-stretch {:display :flex
                               :align-items :.stretch}]
           [:.step {:font-size "30pt"
                    :margin "0 auto"}]
           [:.red {:background-color "#cc0000"
                   :color :white}]
           [:.green {:background-color "#009900"
                     :color :white}]]

          ;; field
          [:.fullsize {:width "100%" :height "100%"
                       :margin 0 :padding 0}
           [:.field {:width "100%" :height "100%"
                     :background-color :darkcyan
                     :border-spacing "1px"}
            [:.day {:background-color :white
                    :-webkit-transition transition}]
            [:.night {:background-color :black
                      :-webkit-transition transition}]

            [:.cell {:cursor :pointer}
             [:.day {:background-color :black
                     :-webkit-transition transition}]
             [:.night {:background-color :yellow
                       :-webkit-transition transition}]
             [:.alive-point {:border-radius "50%"
                             :margin :auto}]]]]
          ;;
          ]]]])]))

(defn input [{:keys [state path] :as params}]
  [:input (merge {:type "number"
                  :class :input
                  :min 1
                  :max 100
                  :step 1
                  :value (str (get-in state path))
                  :on-change #(re-frame/dispatch
                               [::model/set-val path
                                (let [val (js/parseFloat (.. % -target -value))
                                      val* (if (js/isNaN val) 0 val)
                                      ;; mi (:min params)
                                      ma (:max params)]
                                  (cond-> val*
                                    ;; mi (max mi)
                                    ma (min ma)))])}
                 (dissoc params :state :path))])

(defn input-checkbox [{:keys [state path] :as params}]
  [:input (merge {:type "checkbox"
                  ;; :class :input
                  :checked (= "true" (str (get-in state path)))
                  :on-change #(re-frame/dispatch
                               [::model/set-val path (.. % -target -checked)])}
                 (dissoc params :state :path))])

(defn field [{:keys [rows cols field step day-night? area-width area-height]}]
  (when (and (> rows 0) (> cols 0))
    (let [h24 (mod (or step 0) 24)
          day-night (if day-night?
                      (if (<= 6 h24 (dec 18)) :day :night)
                      :day)
          cell-size (min (/ area-height rows) (/ area-width cols))
          point-width-height-style (str (- cell-size 4 #_3) "px")]
      [:div.fullsize
       [:div.square {:style {:width (str (* cell-size cols) "px")
                             :height (str (* cell-size rows) "px")}}
        [:table.field
         [:tbody {:class day-night}
          (for [r (range rows)]
            [:tr {:key r}
             (for [c (range cols)]
               [:td.cell {:key c
                          :on-click #(re-frame/dispatch [::model/invert-cell r c])}
                [:div.alive-point
                 {:class day-night
                  :style {:visibility (if (get field [r c]) :initial :collapse)
                          :width point-width-height-style
                          :height point-width-height-style}}]])])]]]])))

(defn field-fullsize [state]
  (let [loc-state (r/atom nil)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [root (r/dom-node this)]
                               (swap! loc-state assoc
                                      :area-node root
                                      :area-width  (.-clientWidth root)
                                      :area-height (.-clientHeight root))))
      :reagent-render (fn [state] (field (merge state @loc-state)))})))

(defn main-panel []
  (let [{:keys [step movie-on? jump-error history] :as state}
        @(re-frame/subscribe [::model/state])
        step (or step 0)
        running? (> step 0)]
    [:div.page
     [style {:transition-time (if movie-on?
                                (* 7 (/ (:movie-frame-duration-ms state) 1000))
                                5)}]
     [:span.bar
      [:div.header "Game of Life"]
      [:div.splitter]

      [:div.bar-row
       [:label.label "rows:"]
       [input {:type "number" :state state :path [:rows] :min 1 :max 100 :disabled running?}]
       [:label.label] [:label.label "cols:"]
       [input {:type "number" :state state :path [:cols] :min 1 :max 100 :disabled running?}]]
      [:div.bar-row
       [:button {:on-click #(re-frame/dispatch [::model/new-game])} "New game"]]

      [:div.splitter]
      [:div.bar-row
       [:label.label "step:"] [:div.step step]]

      [:div.splitter]

      [:div.bar-row-stretch
       [:button {:on-click #(re-frame/dispatch [::model/step-back])
                 :disabled (empty? history)} "Step back"]
       [:label.label]
       [:button {:on-click #(re-frame/dispatch [::model/step-forward])} "Step forward"]]
      [:div.bar-row
       [:button {:on-click #(re-frame/dispatch [::model/jump-to-step])} "Jump to step"]
       [:label.label]
       [input {:type "number" :state state :path [:step-dest] :min 1 :max 10000}]]
      (when jump-error [:div.bar-row [:label.error jump-error]])

      [:div.splitter]
      [:label.label "frame duration"]
      [:div.bar-row
       [input {:type "range" :class :range :state state :path [:movie-frame-duration-ms]
               :min 100 :max 2000 :step 10}]]
      [:div.bar-row
       [:button.btn {:on-click #(re-frame/dispatch [::model/movie])
                     :class (if movie-on? :red :green)}
        (if movie-on? "Stop movie" "Start movie")]]

      [:div.splitter]
      [:div.bar-row
       [:label.label "day/night"]
       [input-checkbox {:state state :path [:day-night?]}]
       [:div.step (let [h24 (mod step 24)] (str (if (< h24 10) "0") h24 ":00"))]]
      
      ;;
      ]

     [field-fullsize state]]))
