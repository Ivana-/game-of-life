(ns game-of-life.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [game-of-life.model :as model]
   [game-of-life.views :as views]
   [game-of-life.config :as config]
   [re-frisk.core :refer [enable-re-frisk!]]))


(defn dev-setup []
  (when config/debug?
    (enable-re-frisk!)
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::model/initialize-db])
  (dev-setup)
  (mount-root))
