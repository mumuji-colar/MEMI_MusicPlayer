add_library(
    memi_usb_android
    OBJECT
    usb_exclusive_bridge.cpp
    usb_exclusive_device_selection.cpp
    usb_exclusive_device_session.cpp
    usb_exclusive_feature_units.cpp
    usb_exclusive_handle_registry.cpp
    usb_exclusive_recovery.cpp
    usb_exclusive_runtime_report.cpp
    usb_exclusive_transfer_run.cpp
    usb_exclusive_pcm_playback.cpp
    ../feedback/usb_libusb_feedback_backend.cpp
)
memi_configure_owned_native_target(memi_usb_android)
target_link_libraries(memi_usb_android PRIVATE memi_usb_core memi_libusb)
