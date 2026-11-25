add_library(
    memi_dsp_core
    STATIC
    memi_dsp_primitives.cpp
    memi_dsp_filter_stages.cpp
    memi_dsp_space_stages.cpp
    memi_dsp_dynamics_stages.cpp
    memi_dsp_engine.cpp
)
memi_configure_owned_native_target(memi_dsp_core)
# 实时音频线程按速度优化，覆盖通用配置里的 -Oz
target_compile_options(memi_dsp_core PRIVATE "$<$<NOT:$<CONFIG:Debug>>:-O2>")
target_include_directories(memi_dsp_core PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/..")

if(ANDROID)
    add_library(memi_dsp_android OBJECT memi_dsp_jni.cpp)
    memi_configure_owned_native_target(memi_dsp_android)
    target_link_libraries(memi_dsp_android PRIVATE memi_dsp_core)
endif()
