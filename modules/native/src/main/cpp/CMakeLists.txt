cmake_minimum_required(VERSION 3.22.1)

project(memi_native LANGUAGES C CXX)

if(NOT ANDROID)
    message(FATAL_ERROR "Use tests/usb as the source directory for native host tests")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(cmake/MemiNativeCompileOptions.cmake)
add_subdirectory(third_party/libusb)
add_subdirectory(usb)
add_subdirectory(crash)
add_subdirectory(dsp)

# jni methods have no native callers, so include every bridge object explicitly
add_library(
    memi_native
    SHARED
    $<TARGET_OBJECTS:memi_crash>
    $<TARGET_OBJECTS:memi_usb_android>
    $<TARGET_OBJECTS:memi_dsp_android>
)
set_target_properties(memi_native PROPERTIES OUTPUT_NAME "_memi")

find_library(NERI_LOG_LIBRARY log REQUIRED)
find_library(NERI_DL_LIBRARY dl REQUIRED)

target_link_libraries(
    memi_native
    PRIVATE
    memi_usb_core
    memi_dsp_core
    memi_libusb
    android
    ${NERI_LOG_LIBRARY}
    ${NERI_DL_LIBRARY}
)
target_link_options(
    memi_native
    PRIVATE
    -Wl,-z,max-page-size=16384
    "$<$<NOT:$<CONFIG:Debug>>:-Wl,--gc-sections>"
)
