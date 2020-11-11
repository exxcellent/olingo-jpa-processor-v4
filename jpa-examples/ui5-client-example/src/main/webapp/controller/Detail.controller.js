sap.ui.define([
    "sap/base/Log",
    "sap/ui/core/mvc/Controller",
    "sap/ui/core/routing/History",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Log, Controller, History, JSONModel, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("exxcellent.ui5.example.controller.Detail", {

        onInit: function (oEvent) {
            this.router = sap.ui.core.UIComponent.getRouterFor(this);
            this.router.getRoute("personsDetail").attachPatternMatched(this.onObjectMatched, this);

            this.getView().setModel(new JSONModel({
                changedPostalCode: null
            }), "local")

            // Set the initial form to be the display one
            this._showFormFragment("Display");
        },

        onObjectMatched: function (oEvent) {

            const oQuery = oEvent.getParameter("arguments")["?query"];
            this.sPersonId = !!oQuery && oQuery.personId ? decodeURIComponent(oQuery.personId) : null;
            if (this._getFormFragment("Display")) {
                this._getFormFragment("Display").bindElement({
                    path: this.sPersonId,
                    model: "demoModel"
                });
            }
            if (this._getFormFragment("Change")) {
                this._getFormFragment("Change").bindElement({
                    path: this.sPersonId,
                    model: "demoModel"
                });
            }
        },

        resetLocalModel: function () {
            this.getView().getModel("local").setProperty("/changedRegion", null);
            this.getView().getModel("local").setProperty("/changedPostalCode", null);
            this.getView().getModel("local").setProperty("/changedCityName", null);
            this.getView().getModel("local").setProperty("/changedStreetName", null);
            this.getView().getModel("local").setProperty("/changedHouseNumber", null);
        },

        onExit: function () {
            for (var sPropertyName in this._formFragments) {
                if (!this._formFragments.hasOwnProperty(sPropertyName) || this._formFragments[sPropertyName] == null) {
                    return;
                }

                this._formFragments[sPropertyName].destroy();
                this._formFragments[sPropertyName] = null;
            }
        },

        handleEditPress: function () {
            this._toggleButtonsAndView(true);
        },

        handleCancelPress: function () {
            this._toggleButtonsAndView(false);
            this.resetLocalModel();
        },

        handleSavePress: function () {
            this._toggleButtonsAndView(false);
        },

        _formFragments: {},

        _toggleButtonsAndView: function (bEdit) {
            var oView = this.getView();

            // Show the appropriate action buttons
            oView.byId("edit").setVisible(!bEdit);
            oView.byId("save").setVisible(bEdit);
            oView.byId("cancel").setVisible(bEdit);

            // Set the right form type
            this._showFormFragment(bEdit ? "Change" : "Display");
        },

        _getFormFragment: function (sFragmentName) {
            var oFormFragment = this._formFragments[sFragmentName];

            if (oFormFragment) {
                return oFormFragment;
            }

            oFormFragment = sap.ui.xmlfragment(this.getView().getId(), "exxcellent.ui5.example.view." + sFragmentName);

            this._formFragments[sFragmentName] = oFormFragment;
            return this._formFragments[sFragmentName];
        },

        _showFormFragment: function (sFragmentName) {
            const oPage = this.byId("page");

            oPage.removeAllContent();
            oPage.insertContent(this._getFormFragment(sFragmentName));
        },

        onNavBack: function () {
            const oHistory = History.getInstance();
            const sPreviousHash = oHistory.getPreviousHash();

            if (sPreviousHash !== undefined) {
                window.history.go(-1);
            } else {
                this.router.navTo("persons", true);
            }
            this.resetLocalModel();
        },

        onSaveBusinessPartner: function (oEvent) {
            // get Data
            const oLocalModel = this.getView().getModel("local");
            const sChangedRegion = oLocalModel.getProperty("/changedRegion");
            const sChangedPostalCode = oLocalModel.getProperty("/changedPostalCode");
            const sChangedCityName = oLocalModel.getProperty("/changedCityName");
            const sChangedStreetName = oLocalModel.getProperty("/changedStreetName");
            const sChangedHouseNumber = oLocalModel.getProperty("/changedHouseNumber");

            var oOdataModel = this.getView().getModel("demoModel");
            var oContext = this._getFormFragment("Change").getBindingContext("demoModel");
            var oAction = oOdataModel.bindContext("org.apache.olingo.jpa.modifyBusinessPartner(...)", oContext);
            let oRegionParameter = !!sChangedRegion ? oAction.getParameterContext().setProperty("changedRegion", sChangedRegion) : Promise.resolve();
            let oPostalParameter = !!sChangedPostalCode ? oAction.getParameterContext().setProperty("changedPostalCode", sChangedPostalCode) : Promise.resolve();
            let oCityParameter = !!sChangedCityName ? oAction.getParameterContext().setProperty("changedCityName", sChangedCityName) : Promise.resolve();
            let oStreetParameter = !!sChangedStreetName ? oAction.getParameterContext().setProperty("changedStreetName", sChangedStreetName) : Promise.resolve();
            let oHouseNumberParameter = !!sChangedHouseNumber ? oAction.getParameterContext().setProperty("changedHouseNumber", sChangedHouseNumber) : Promise.resolve();

            Promise.all([oRegionParameter, oPostalParameter, oCityParameter, oStreetParameter, oHouseNumberParameter]).then((values) => {
                oAction.execute().then(
                    () => {
                        oOdataModel.refresh();
                        this._toggleButtonsAndView(false);
                        this.resetLocalModel();
                        MessageToast.show("Data saved for Business Partner ID: " + oContext.getProperty("ID"));
                    },
                    (oError) => {
                        MessageBox.alert(oError.message, {
                            icon: MessageBox.Icon.ERROR,
                            title: "Error"
                        });
                    });
            });
        }

    });

});