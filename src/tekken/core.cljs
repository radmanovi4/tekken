;; TODO:
;;
;; - pages
;; - codemirror markdown editor
;; - proper mm styles
;; - generate printable html file (print view)
;; - options component
;; - generate variants
;; - generate answer-sheets
;; - generate answer-keys
;;
;; - image recognition
;;
;; - generate checkui
;; - generate statistics
;;

(ns tekken.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require  [tekken.util :as util]
               [om.core :as om :include-macros true]
               [om.dom :as dom :include-macros true]
               [cljs.core.async :refer [put! <! >! chan map>]]))

;; ============================================================================
;; Data

(def template
  (.-innerText (util/$ "#template")))

(defonce data
  (atom {:test-data {:title "Clojure II"
                     :questions {}
                     :variants 5
                     :per-variant 15}}))

;; ============================================================================
;; Components

(defn editor
  [{:keys [test-data]} owner]
  (reify
    om/IInitState
    (init-state
      [_]
     {:text template
      :onChange
      (fn [_]
        (let [value (->> (om/get-node owner "text")
                         (.-value))]
          (om/set-state! owner :text value)
          (om/update! test-data :questions (util/md->edn value))))})

    om/IDidMount
    (did-mount
     [_]
     ((om/get-state owner :onChange)))

    om/IRenderState
    (render-state
      [_ {:keys [text onChange]}]
      (dom/form
        #js {:action "javascript: void(0);"
             :id "editor"}
        (dom/textarea
          #js {:type "text"
               :ref "text"
               :rows "20"
               :cols "30"
               :value text
               :onChange onChange})))))

(defn viewer
  "Test viewer component."
  [{:keys [test-data]} owner]
  (reify
    om/IInitState
    (init-state
      [_]
     {:onClick
      (fn [_]
        (let [ch (util/html->canvas (om/get-node owner "page"))]
          (go (->> (<! ch)
                   (util/canvas->pdf)
                   (aset js/window "location")))))
      :onMouseEnter
      (fn [_]
        (let [ch (util/html->canvas (om/get-node owner "page"))]
          (go (let [cvs (<! ch)
                    node (om/get-node owner "preview")]
                (aset node "innerHTML" "")
                (.appendChild node cvs)))))

      :onMouseLeave
      (fn [_]
        (-> (om/get-node owner "preview")
            (aset "innerHTML" "")))})

    om/IDidUpdate
    (did-update
     [_ _ _]
     ;;
     (js/pagify))

    om/IRenderState
    (render-state
      [_ {:keys [onClick onMouseEnter onMouseLeave]}]
     (dom/div
      #js {:id "viewer"}
      (dom/a #js {:href "javascript:void(0);"
                  :onClick onClick
                  :onMouseEnter onMouseEnter
                  :onMouseLeave onMouseLeave} "Preview/Download")
      (dom/div #js {:ref "preview"
                    :className "preview"})
      (dom/section
       (clj->js {:ref "page"
                 :className "page"
                 :dangerouslySetInnerHTML
                 {:__html (util/edn->html (:questions test-data))}}))))))

(defn home-ui
  "Home page ui."
  [app owner]
  (reify
    om/IRenderState
    (render-state
     [_ {:keys [ch]}]
     (dom/section
      #js {:className "home"}
      (om/build editor app)
      (om/build viewer app)))))

(defn tekken
  "The big boss that builds all the components together."
  []
  (om/root
    (fn [app owner]
      (dom/div
        nil
        (om/build home-ui app)))
    data
    {:target (. js/document (getElementById "main"))}))

(tekken)
