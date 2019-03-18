(ns net.joelane.tabella
  (:gen-class)
  (:require [cljfx.api :as fx]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(def init-state {:typed-text ""
                 :by-id      {0 {:id     0
                                 :text   "(+ 1 1)"
                                 :result ""}
                              1 {:id     1
                                 :text   "(defn foo [a b] (+ a b))"
                                 :result ""}
                              2 {:id     2
                                 :text   "(foo 3 4)"
                                 :result ""}}})

(def *state (atom (fx/create-context init-state)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn todo-view [{:keys [text id]}]
  {:fx/type  :h-box
   :spacing  5
   :padding  5
   :children [{:fx/type         :text-area
               :style           {:-fx-font-family "monospace"}
               :text            text
               :on-text-changed #(swap! *state fx/swap-context assoc-in [:by-id id :text] %)}
              {:fx/type   :label
               :wrap-text true
               :style     {:-fx-font-family "monospace"}
               :text      (when-not (str/blank? text)
                            (with-out-str
                              (try
                                (let [form (read-string text)]
                                  (pp/pprint (eval form)))
                                (catch Exception e
                                  (pp/pprint e)))))}]})
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn root [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type     :v-box
                       :pref-width  600
                       :pref-height 900
                       :children    [{:fx/type      :scroll-pane
                                      :v-box/vgrow  :always
                                      :fit-to-width true
                                      :content      {:fx/type  :v-box
                                                     :children (->> (fx/sub context :by-id)
                                                                    vals
                                                                    (sort-by :id)
                                                                    (map #(assoc %
                                                                            :fx/type todo-view
                                                                            :fx/key (:id %))))}}
                                     {:fx/type         :text-area
                                      :v-box/margin    5
                                      :text            (fx/sub context :typed-text)
                                      :prompt-text     "Add new todo and press ENTER"
                                      :on-text-changed #(swap! *state fx/swap-context assoc :typed-text %)
                                      :on-key-pressed  (fn [event]
                                                         (when (= KeyCode/ENTER (.getCode ^KeyEvent event))
                                                           (swap! *state fx/swap-context #(-> %
                                                                                              (assoc :typed-text "")
                                                                                              (assoc-in [:by-id (count (:by-id %))]
                                                                                                        {:id   (count (:by-id %))
                                                                                                         :text (:typed-text %)})))))}]}}})
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def renderer
  (fx/create-renderer
    :middleware (comp
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_]
                                      {:fx/type root})))
    :opts {:fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn ui []
  (fx/mount-renderer *state renderer))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ui))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
