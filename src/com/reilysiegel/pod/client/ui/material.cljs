(ns com.reilysiegel.pod.client.ui.material
  (:require ["@material-ui/core" :as mui]
            ["@material-ui/lab/TabContext" :default TabContext]
            ["@material-ui/lab/TabList" :default TabList]
            ["@material-ui/lab/TabPanel" :default TabPanel]
            ["@material-ui/core/styles" :as styles]
            ["@material-ui/icons/Add" :default AddIcon]
            ["@material-ui/icons/Dashboard" :default DashboardIcon]
            ["@material-ui/icons/Delete" :default DeleteIcon]
            ["@material-ui/icons/ExpandMore" :default ExpandMoreIcon]
            ["@material-ui/icons/Menu" :default MenuIcon]
            ["@material-ui/icons/People" :default PeopleIcon]
            ["@material-ui/icons/AssignmentTurnedIn"
             :default AssignmentTurnedInIcon]
            ["@material-ui/icons/AssignmentInd" :default AssignmentIndIcon]
            ["@material-ui/icons/AssignmentLate" :default AssignmentLateIcon]
            ["@material-ui/icons/AssignmentReturn" :default AssignmentReturnIcon]
            ["@material-ui/icons/AssignmentReturned" :default AssignmentReturnedIcon]
            [com.fulcrologic.fulcro.algorithms.react-interop :as interop])
  (:refer-clojure :exclude [list]))

(def alert (interop/react-factory mui/Alert))
(def app-bar (interop/react-factory mui/AppBar))
(def box (interop/react-factory mui/Box))
(def button (interop/react-factory mui/Button))
(def card (interop/react-factory mui/Card))
(def card-actions (interop/react-factory mui/CardActions))
(def card-content (interop/react-factory mui/CardContent))
(def checkbox (interop/react-factory mui/Checkbox))
(def collapse (interop/react-factory mui/Collapse))
(def container (interop/react-factory mui/Container))
(def css-baseline (interop/react-factory mui/CssBaseline))
(def dialog (interop/react-factory mui/Dialog))
(def dialog-actions (interop/react-factory mui/DialogActions))
(def dialog-content (interop/react-factory mui/DialogContent))
(def drawer (interop/react-factory mui/Drawer))
(def fab (interop/react-factory mui/Fab))
(def form-control-label (interop/react-factory mui/FormControlLabel))
(def grid (interop/react-factory mui/Grid))
(def icon-button (interop/react-factory mui/IconButton))
(def linear-progress (interop/react-factory mui/LinearProgress))
(def list (interop/react-factory mui/List))
(def list-item (interop/react-factory mui/ListItem))
(def list-item-icon (interop/react-factory mui/ListItemIcon))
(def list-item-text (interop/react-factory mui/ListItemText))
(def snackbar (interop/react-factory mui/Snackbar))
(def tab (interop/react-factory mui/Tab))
(def tabs (interop/react-factory mui/Tabs))
(def tab-context (interop/react-factory TabContext))
(def tab-list (interop/react-factory TabList))
(def tab-panel (interop/react-factory TabPanel))
(def text-field (interop/react-input-factory mui/TextField))
(def toolbar (interop/react-factory mui/Toolbar))
(def tooltip (interop/react-factory mui/Tooltip))
(def typography (interop/react-factory mui/Typography))

(def add-icon (interop/react-factory AddIcon))
(def dashboard-icon (interop/react-factory DashboardIcon))
(def delete-icon (interop/react-factory DeleteIcon))
(def expand-more-icon (interop/react-factory ExpandMoreIcon))
(def menu-icon (interop/react-factory MenuIcon))
(def people-icon (interop/react-factory PeopleIcon))
(def assignment-turned-in-icon (interop/react-factory AssignmentTurnedInIcon))
(def assignment-ind-icon (interop/react-factory AssignmentIndIcon))
(def assignment-late-icon (interop/react-factory AssignmentLateIcon))
(def assignment-return-icon (interop/react-factory AssignmentReturnIcon))
(def assignment-returned-icon (interop/react-factory AssignmentReturnedIcon))

(def styles-provider (interop/react-factory mui/StylesProvider))
(def theme-provider (interop/react-factory  styles/ThemeProvider))
(def create-mui-theme (comp styles/createMuiTheme clj->js))
(def fade styles/fade)

(defn make-styles [f]
  (comp
   #(js->clj % :keywordize-keys true)
   (mui/makeStyles (fn [theme]
                     (clj->js (f (js->clj theme :keywordize-keys true)))))))


(def use-media-query mui/useMediaQuery)
(defn use-theme []
  (js->clj (styles/useTheme) :keywordize-keys true))

(defn use-breakpoint
  ([key direction]
   (let [{{:keys [up down]} :breakpoints} (use-theme)]
     (use-media-query ((direction {:up   up
                                   :down down})
                       (name key)))))
  ([]
   (let [{{:keys [keys up]} :breakpoints} (use-theme)
         res                              (->> keys
                                               (map (comp use-media-query up))
                                               reverse
                                               (drop-while false?))]
     (or (some->> keys
                  reverse
                  (drop (- (count keys) (count res)))
                  first
                  keyword)
         :xs))))
