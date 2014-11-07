var require = {
    baseUrl: "/scripts/",
    paths: {
        jquery:                     "vendor/jquery",
        jqueryui:                   "vendor/jquery-ui-1.10.1.custom.min",
        d3:                         "vendor/d3.v3.min",

        react:                      "vendor/react",
        JSXTransformer:             "vendor/JSXTransformer-0.4.0",
        jsx:                        "vendor/jsx-0.4.0",
        text:                       "vendor/text",

        'foundation.core':          "vendor/foundation/foundation",
        'foundation.abide':         "vendor/foundation/foundation.abide",
        'foundation.accordian':     "vendor/foundation/foundation.accordian",
        'foundation.alert':         "vendor/foundation/foundation.alert",
        'foundation.clearing':      "vendor/foundation/foundation.clearing",
        'foundation.dropdown':      "vendor/foundation/foundation.dropdown",
        'foundation.equalizer':     "vendor/foundation/foundation.equalizer",
        'foundation.interchange':   "vendor/foundation/foundation.interchange",
        'foundation.joyride':       "vendor/foundation/foundation.joyride",
        'foundation.magellan':      "vendor/foundation/foundation.magellan",
        'foundation.offcanvas':     "vendor/foundation/foundation.offcanvas",
        'foundation.orbit':         "vendor/foundation/foundation.orbit",
        'foundation.reveal':        "vendor/foundation/foundation.reveal",
        'foundation.slider':        "vendor/foundation/foundation.slider",
        'foundation.tab':           "vendor/foundation/foundation.tab",
        'foundation.tooltip':       "vendor/foundation/foundation.tooltip",
        'foundation.topbar':        "vendor/foundation/foundation.topbar",
        'jquery.cookie':            "vendor/jquery.cookie",
        modernizr:                  "vendor/modernizr",
        fastclick:                  "vendor/fastclick",
        placeholder:                "vendor/placeholder",
        underscore:                 "vendor/underscore",
        uuid:                       "vendor/uuid"
    },
    shim: {
        jqueryui: {
            deps: ["jquery"]
        },
        jquery: {
            exports: "jquery"
        },
        d3 : {
            exports: "d3"
        },
        react: {
            exports: 'React'
        },
        /* Foundation */
        'foundation.core': {
            deps: [
                'jquery',
                'modernizr'
            ],
            exports: 'Foundation'
        },
        'foundation.abide': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.accordion': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.alert': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.clearing': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.dropdown': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.equalizer': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.interchange': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.joyride': {
            deps: [
                'foundation.core',
                'jquery.cookie'
            ]
        },
        'foundation.magellan': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.offcanvas': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.orbit': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.reveal': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.slider': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.tab': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.tooltip': {
            deps: [
                'foundation.core'
            ]
        },
        'foundation.topbar': {
            deps: [
                'foundation.core'
            ]
        },
        'jquery.cookie': {
            deps: [
                'jquery'
            ]
        },
        'fastclick': {
            exports: 'FastClick'
        },
        'modernizr': {
            exports: 'Modernizr'
        },
        'placeholder': {
            exports: 'Placeholder'
        }
    },
    jsx: {
        fileExtension: '.jsx'
    }
};
