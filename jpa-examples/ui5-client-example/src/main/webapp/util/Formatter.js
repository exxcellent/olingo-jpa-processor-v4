sap.ui.define([], function () {
    'use strict';

    const getI18nResourceBundle = function (uiElement) {
        const component = uiElement.getOwnerComponent();
        if (!component) {
            return null;
        }
        const i18nModel = component.getModel('i18n');
        if (!i18nModel) {
            return null;
        }
        return i18nModel.getResourceBundle();
    };

    const formatRoles = function (aRoles) {
        return aRoles.join(',');
    };

    return {

        i18nResourceBundle: getI18nResourceBundle,

        formatRoles: formatRoles
    };
});