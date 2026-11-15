# Default filter Evidence ledger

REA/JADX 0.7.1. Complete wrappers, source, warnings and limitations are retained in ignored `reference_app/<version>/default-filter/`. Obfuscated names are version-specific. A missing class/method during a version change was resolved through class inspection; absence was not inferred from that lookup.

`316-selected.json` is an unsuccessful selector migration: `data.pref.d.m0` now reads recent searches. It is retained for audit and is **not evidence for the default filter**. The constants, settings constructor, product widget, renderer and shader below establish that chain. `ev_1395...` and `render-frame.json` use complete smali fallback; read control flow from instructions rather than the failed Java decompilation.

- v10.0.311 base SHA-256: `45ddfcecd097aa3c3b557f43cab42df9bdc2c5e3709475bdeb2ed2b621de9893`.
- v10.0.316 base SHA-256: `b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.

| Version | Retained file | Subject | Evidence ID |
| --- | --- | --- | --- |
| v10.0.311 | `ev_1395e59c5e2b2490ef72b094bf61283d2a62ec91a01d1f2fc4e6d71230308082.json` | `CameraGLSurfaceView.onDrawFrame` | `ev_1395e59c5e2b2490ef72b094bf61283d2a62ec91a01d1f2fc4e6d71230308082` |
| v10.0.311 | `render-frame.json` | `com.xhey.xcamerasdk.render.b.c` | `ev_f9d88e9a6a96a5e8a129e3ec8b51e061c1c32dc1d03f9a76942d16659c70120d` |
| v10.0.311 | `ev_90d9ce2060e5a933ad939fbe6945f72ab63a95e0a9c0db10ec6406ea8e5bcc52.json` | `com.xhey.xcamerasdk.render.b.d` | `ev_90d9ce2060e5a933ad939fbe6945f72ab63a95e0a9c0db10ec6406ea8e5bcc52` |
| v10.0.311 | `app-default-constants.json` | `com.xhey.xcamera.b.class` | `ev_dafa59519c452689edf8d29da41bda47044a28d207c110bc95538e4e38f10fc9` |
| v10.0.311 | `beauty-classes.json` | `.class` | `ev_f7b97c0a951a37a4d0f586e224e4538171c304a75b0dd878addfafdb698a05ce` |
| v10.0.311 | `beauty-constants.json` | `com.softsugar.sensear.params.STEffectBeautyParams.class` | `ev_3b23ccab41c317f489b73f1a64f8e322d5df116987468bbe2310381b11198dcd` |
| v10.0.311 | `beauty-default-policy.json` | `b10.r.c` | `ev_d56cfed654f465069719f6ba14edc29020cc5f05c3e7680d79d65af57254f9c1` |
| v10.0.311 | `beauty-defaults.json` | `h10.d.j` | `ev_136367dd39bc64f61664ee691c4e0abf72c2d067e9b938aa7e1434f343c7b8b5` |
| v10.0.311 | `beauty-enable-policy.json` | `b10.r.d` | `ev_6789dffb011eee51e2a0b066a093634f421aa860ff83f0567408055fe2e04ee2` |
| v10.0.311 | `beauty-formal-settings.json` | `b10.q.h` | `ev_b24e72e7779d86a2182a6556b1f2b9e161b0ef41b8afac293b11b283681e440f` |
| v10.0.311 | `beauty-frame-gate.json` | `com.xhey.xcamerasdk.render.b.e` | `ev_2ec0b23f71a73d7bc440ad3d8e1f5194fbad9f12700ee1251388e647ef4c815e` |
| v10.0.311 | `beauty-init.json` | `com.xhey.sdk.beauty.BeautyParamsCacheModel.<init>` | `ev_2852aeb2fb5b56f651c3f1101c797d22db261b11f6a94a34ed978333749c6a0f` |
| v10.0.311 | `beauty-model-readers.json` | `com.xhey.sdk.beauty.BeautyParamsCacheModel.class` | `ev_2e89e4aebdd5493938269c94dbce418dcc6f515ce1bb160a6dcbad9189060fa9` |
| v10.0.311 | `beauty-model.json` | `com.xhey.sdk.beauty.BeautyParamsCacheModel.class` | `ev_bf2f6b323d7600f1fda98c9541359b09c467d4853e431b11872b1ff5dbb51e9b` |
| v10.0.311 | `beauty-native-gates.json` | `com.xhey.xcamerasdk.render.n.i` | `ev_331e46865648eaf9a2b010cb459571bfd3e644f9f077d0c5a3233f1a82db8947` |
| v10.0.311 | `beauty-native-strength.json` | `com.xhey.xcamerasdk.render.n.g` | `ev_cd04f11c354ad8c1b43cbd71a8d1e389e6e81f7d8803c7b6c9d29837763e6157` |
| v10.0.311 | `beauty-rear-reset.json` | `com.xhey.xcamerasdk.render.n.f` | `ev_24fea665e955c6d632bbf26cbf641d2620578e8d37e54bec69d6f645efed9b35` |
| v10.0.311 | `beauty-render.json` | `com.xhey.xcamerasdk.render.n.e` | `ev_2df11e3eba2214ddc37a3af593795a007773d61c05e933a6e45c663b8da14ad8` |
| v10.0.311 | `beauty-renderer.json` | `com.xhey.xcamerasdk.render.n.class` | `ev_3fe735dc3cd5a16eb2e26b7ed30bb3e259827e98c04867c4066475435b6c9d1b` |
| v10.0.311 | `camera-facing-update.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.q` | `ev_c3734ff2d6e33d1cfcb717148c7970a9e2b3fa578b431892e8ee010ad56e186c` |
| v10.0.311 | `camera-gl-methods.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.class` | `ev_d53d46e0b40e07d1203486e5e48da72d477042228a17a100b686667309a4eb43` |
| v10.0.311 | `camera-scale-update.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.l` | `ev_edc6616190146f361bd20acbaf5756c4f9ea25c887fda6733a7acb27d3a16943` |
| v10.0.311 | `camera-state-update.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.m` | `ev_a907e46d063fa9316d0aee9208236d716e68b3690e9ca86a44026461bfd67335` |
| v10.0.311 | `default-constants-init.json` | `com.xhey.xcamera.b.<clinit>` | `ev_fcbd73b22ecaa45ac0bac906e99c79810e08853576b5ee3f005241b7b2865a99` |
| v10.0.311 | `default-selected.json` | `com.xhey.xcamera.data.pref.d.m0` | `ev_4cb3d7566040cf62664861397741a1d592f3298e01c2514c31e0dfb2ab734f11` |
| v10.0.311 | `defaults-init.json` | `h10.c.<clinit>` | `ev_6bb4d8e54c40b846243d903d5bf8d39779dac85c3edb2ada151616ab1d825583` |
| v10.0.311 | `frame-configuration.json` | `com.xhey.xcamerasdk.ui.a.class` | `ev_3ad318a6be8de7315641cc366052a87e33816676ab761507c25210b644aec855` |
| v10.0.311 | `frame-route-setup.json` | `com.xhey.xcamerasdk.ui.a.run` | `ev_77bd850ed730af1c6ff30019952daab0f07d1d144b21dd30741b43e3aa313cb5` |
| v10.0.311 | `gl-close-effect.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.setCloseAllEffectForPreview` | `ev_ae43f8860d2dea2c39cd9b6d7bc6d21785bff21d98f61697457477a984e1d4e9` |
| v10.0.311 | `gl-effect-switch.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.o` | `ev_4858f4f8c5500326f56e1b9dc80447f0b31267c9807ed9b306accbe944778778` |
| v10.0.311 | `gl-surface-init.json` | `com.xhey.xcamerasdk.ui.CameraGLSurfaceView.onSurfaceCreated` | `ev_1a99fc1685c7489789874aea78736447b995a00b13b59b7328c738f37a417bb1` |
| v10.0.311 | `lut-shader.json` | `sc0.a.class` | `ev_c59f01fdffa2e12433858d4d929f11afee068c96840b840e5b7e73042bb309b7` |
| v10.0.311 | `settings-init.json` | `h10.b.<init>` | `ev_6a44f1acb2a99c1911d47b4f88e04ad7d1d3488c823bf9e3ad34e586127b2c4d` |
| v10.0.311 | `settings-load.json` | `h10.d.h` | `ev_68ab0b26901f7591be3e7dfbf0adc77afc0d3232f57cfc4d9653c7fa8e372347` |
| v10.0.311 | `settings-model.json` | `h10.b.class` | `ev_3dce6e4c15248f3a725f6e9bf4d4f73be5226f6f93b156d1e7d30fb351a7f40a` |
| v10.0.311 | `shader-draw.json` | `sc0.a.k` | `ev_2578a919360fdbd79b9eefba3c0ee6c513670cf8bf850c217f9163616581da70` |
| v10.0.311 | `shader-gate.json` | `sc0.a.l` | `ev_473a324ca838a164b982e38b333eccbbd70d853352f944d11464bcdadfacba63` |
| v10.0.311 | `shader-init.json` | `sc0.a.<init>` | `ev_f0cbb9dc723945252cbb98621b603e725fb029876ced1c6215d0b03c26a378c8` |
| v10.0.311 | `shader-source.json` | `sc0.a.j` | `ev_63223342b77bc5e18323ac0c2483102f024a830c726093a36b210f331de7a84d` |
| v10.0.311 | `shader-state-readers.json` | `com.xhey.xcamerasdk.render.b.class` | `ev_a7fdafb51ef0417463b8079af12351fadfd4ab8c35ae95549643e3bdf395701b` |
| v10.0.311 | `shader-uniforms.json` | `sc0.a.h` | `ev_3aa9e5a4de9c7719239d0fcec8b1f1ca052901a21afcf3820c97ed63a09ed8c6` |
| v10.0.316 | `316-app-defaults.json` | `com.xhey.xcamera.b.<clinit>` | `ev_198927c4a25faae2653fb93e65c2d15e3465dab7c2a64628d62323eb5cd3d863` |
| v10.0.316 | `316-constants-refs.json` | `com.xhey.xcamera.b.class` | `ev_646621494985d5b0e254b294a7b02aba1791ba38013fb520076be0f23257b067` |
| v10.0.316 | `316-frame-class.json` | `com.xhey.xcamerasdk.render.b.class` | `ev_266457203d7411479f66ee77bb2faad3f561e8e8816856a0e67e902ef2af44f0` |
| v10.0.316 | `316-lut-dispatch.json` | `com.xhey.xcamerasdk.render.b.d` | `ev_510ed07e6a6e821c1df87e3280d13af1aaa62b3be90085083c4f32d6a22072a0` |
| v10.0.316 | `316-selected.json` | `com.xhey.xcamera.data.pref.d.m0` | `ev_bf4f1595bb79d9074c5b4282f2a15c2a37813491ac7662b058e59d4c5481ff4e` |
| v10.0.316 | `316-settings-class.json` | `h10.b.class` | `ev_0d7c67a95dd93183ceea44ccbe707974e1adaef51bb3864944693d6c4d77b061` |
| v10.0.316 | `316-settings-init.json` | `h10.b.<init>` | `ev_5cd45475dcd6b16520b4a0367b279f8c19b635ef52c751e138cc6771c7e79f8a` |
| v10.0.316 | `316-shader-source.json` | `tc0.a.j` | `ev_aa90ca5061d2eb409730458c590374e6b29180b5b069d44c7dc8e4680aa162b9` |
| v10.0.316 | `316-shader-uniforms.json` | `tc0.a.h` | `ev_eb7db5f8fcc1a0d8ebacf9c1fd4239eab0c33c9cd52aadbb658776d6deb14ade` |
| v10.0.316 | `316-widget-filter.json` | `com.xhey.xcamera.ui.camera.picNew.CameraWidget.N` | `ev_6b7fe0bca809edce62392f3532c89c7942b86aa27637fcdf5f6e851d1de89f56` |
