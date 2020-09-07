sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator",
    "sap/m/ToolbarSpacer",
    "exxcellent/ui5/example/util/Formatter"
], function (Controller, JSONModel, MessageToast, Filter, FilterOperator, ToolbarSpacer, Formatter) {
    "use strict";

    return Controller.extend("exxcellent.ui5.example.controller.OverviewTable", {

        formatter: Formatter,

        onInit: function () {
            var oView = this.getView();

            oView.setModel(new JSONModel({
                globalFilter: ""
            }), "ui");

            this.router = sap.ui.core.UIComponent.getRouterFor(this);

            this._oGlobalFilter = null;
        },

        _filter: function () {
            var oFilter = null;

            if (this._oGlobalFilter) {
                oFilter = new Filter([this._oGlobalFilter], true);
            } else if (this._oGlobalFilter) {
                oFilter = this._oGlobalFilter;
            }

            this.byId("table").getBinding("rows").filter(oFilter, "Application");
        },

        filterGlobally: function (oEvent) {
            var sQuery = oEvent.getParameter("query");
            this._oGlobalFilter = null;

            if (sQuery) {
                this._oGlobalFilter = new Filter([
                    new Filter("Name", FilterOperator.Contains, sQuery),
                    new Filter("Category", FilterOperator.Contains, sQuery)
                ], false);
            }

            this._filter();
        },

        clearAllFilters: function (oEvent) {
            var oTable = this.byId("table");

            var oUiModel = this.getView().getModel("ui");
            oUiModel.setProperty("/globalFilter", "");

            this._oGlobalFilter = null;
            this._filter();

            var aColumns = oTable.getColumns();
            for (var i = 0; i < aColumns.length; i++) {
                oTable.filter(aColumns[i], null);
            }
        },

        onSelect: function (oEvent) {
            if (oEvent.getParameter("rowContext")) {
                this.router.navTo("personsDetail", {query: {personId: encodeURIComponent(oEvent.getParameter("rowContext").sPath)}});
            }
        }

    });

});