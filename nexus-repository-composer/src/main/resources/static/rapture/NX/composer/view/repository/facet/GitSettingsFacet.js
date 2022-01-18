/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Configuration specific to proxy repositories.
 *
 * @since 3.0
 */
Ext.define('NX.composer.view.repository.facet.GitSettingsFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-composer-repository-gitsettings-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    allowBlank: false,
//    itemCls: 'required-field'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        itemId: 'gitSettingsFieldSet',
        cls: 'nx-form-section',
        title: 'Git Mirroring Settings',
        items: [
          {
            xtype: 'nx-optionalfieldset',
            title: 'Enable git mirroring',
            checkboxToggle: true,
            checkboxName: 'attributes.gitSettings.enabled',
            collapsed: true,
            items: [
              {
                xtype: 'nx-url',
                itemId: 'attributes_gitSettings_remoteUrl',
                name: 'attributes.gitSettings.remoteUrl',
                fieldLabel: 'Remote Url',
                emptyText: 'https://gitlab.com/username/',
                allowBlank: false,
              },
              {
                xtype: 'textfield',
                itemId: 'attributes_gitSettings_username',
                name: 'attributes.gitSettings.username',
                fieldLabel: 'Username',
                allowBlank: false,
              },
              {
                xtype:'nx-password',
                itemId: 'attributes_gitSettings_password',
                name: 'attributes.gitSettings.password',
                fieldLabel: 'Password',
                allowBlank: true
              },
              {
                xtype: 'nx-optionalfieldset',
                title: NX.I18n.get('System_HttpSettings_Proxy_Title'),
                checkboxToggle: true,
                checkboxName: 'attributes.gitSettings.proxy.httpEnabled',
                collapsed: true,
                items: [
                  {
                    xtype: 'nx-hostname',
                    itemId: 'attributes_gitSettings_proxy_httpHost',
                    name: 'attributes.gitSettings.proxy.httpHost',
                    fieldLabel: NX.I18n.get('System_HttpSettings_ProxyHost_FieldLabel'),
                    helpText: NX.I18n.get('System_HttpSettings_ProxyHost_HelpText'),
                    allowBlank: false
                  },
                  {
                    xtype: 'numberfield',
                    itemId: 'attributes_gitSettings_proxy_httpPort',
                    name: 'attributes.gitSettings.proxy.httpPort',
                    fieldLabel: NX.I18n.get('System_HttpSettings_ProxyPort_FieldLabel'),
                    minValue: 1,
                    maxValue: 65535,
                    allowDecimals: false,
                    allowExponential: false,
                    allowBlank: false
                  },
                  {
                    xtype: 'textfield',
                    itemId: 'attributes_gitSettings_proxy_httpUsername',
                    name: 'attributes.gitSettings.proxy.httpUsername',
                    fieldLabel: 'Username',
                  },
                  {
                    xtype:'nx-password',
                    itemId: 'attributes_gitSettings_proxy_httpPassword',
                    name: 'attributes.gitSettings.proxy.httpPassword',
                    fieldLabel: 'Password',
                    allowBlank: true
                  }
                ]
              },
              {
                xtype: 'nx-optionalfieldset',
                title: NX.I18n.get('System_HttpSettings_HttpsProxy_Title'),
                itemId: 'httpsProxy',
                checkboxToggle: true,
                checkboxName: 'attributes.gitSettings.proxy.httpsEnabled',
                collapsed: true,
                items: [
                  {
                    xtype: 'nx-hostname',
                    itemId: 'attributes_gitSettings_proxy_httpsHost',
                    name: 'attributes.gitSettings.proxy.httpsHost',
                    fieldLabel: NX.I18n.get('System_HttpSettings_HttpsProxyHost_FieldLabel'),
                    helpText: NX.I18n.get('System_HttpSettings_HttpsProxyHost_HelpText'),
                    allowBlank: false
                  },
                  {
                    xtype: 'numberfield',
                    itemId: 'attributes_gitSettings_proxy_httpsPort',
                    name: 'attributes.gitSettings.proxy.httpsPort',
                    fieldLabel: NX.I18n.get('System_HttpSettings_HttpsProxyPort_FieldLabel'),
                    minValue: 1,
                    maxValue: 65535,
                    allowDecimals: false,
                    allowExponential: false,
                    allowBlank: false
                  },
                  {
                    xtype: 'textfield',
                    itemId: 'attributes_gitSettings_proxy_httpsUsername',
                    name: 'attributes.gitSettings.proxy.httpsUsername',
                    fieldLabel: 'Username',
                  },
                  {
                    xtype:'nx-password',
                    itemId: 'attributes_gitSettings_proxy_httpsPassword',
                    name: 'attributes.gitSettings.proxy.httpsPassword',
                    fieldLabel: 'Password',
                    allowBlank: true
                  }
                ]
              },
              {
                xtype: 'nx-valueset',
                name: 'attributes.gitSettings.proxy.nonProxyHosts',
                itemId: 'attributes_gitSettings_proxy_nonProxyHosts',
                fieldLabel: NX.I18n.get('System_HttpSettings_ExcludeHosts_FieldLabel'),
                helpText: NX.I18n.get('System_HttpSettings_ExcludeHosts_HelpText'),
                sorted: true,
                allowBlank: true
              }
            ]
          }
        ]
      }
    ];

    me.callParent();
  }

});
