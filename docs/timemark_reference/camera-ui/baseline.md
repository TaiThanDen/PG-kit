# Camera UI baseline and ownership

Camera UI work started from commit `435ca0a`'s predecessor state while the shared checkout already contained uncommitted video changes. Original baseline copies are retained in ignored `reference_app/v10.0.311/camera-ui/CameraScreen.baseline.java` and `CameraGestureController.baseline.java`.

Owned implementation: CameraScreen camera controls, CameraGestureController, FocusRingView, FocusExposureView and the camera vector icons. Existing photo/video tab and recording changes in CameraScreen belong to the earlier video work and must remain unstaged by this chat.

CameraEngine and MainActivity are shared. `shared-camera-hooks.patch` is a proposed small integration patch; it has not been applied. It resets exposure on a new focus and supplies the actual zoom range so unsupported presets can be hidden. Apply it after coordinating those shared files. It does not touch video methods, orientation, renderer or templates.

Activity stays portrait. Preview and ImageCapture rotation sources are unchanged. Production time remains the device time.
