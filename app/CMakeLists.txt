cmake_minimum_required(VERSION 3.4.1)

set(ProjectRoot /home/xulei/AndroidStudioProjects/refocus/)

add_library( jni_arcsoft_real_bokeh SHARED src/main/cpp/arcsoft_realtime/arcrealbokeh.cpp  )

find_library( log-lib log )

add_library( libmpbase  SHARED IMPORTED )
set_target_properties( libmpbase PROPERTIES IMPORTED_LOCATION ${ProjectRoot}/app/libs/x86_64/libmpbase.so)

add_library( libarcsoft_dualcam_refocus  SHARED IMPORTED )
set_target_properties( libarcsoft_dualcam_refocus PROPERTIES IMPORTED_LOCATION ${ProjectRoot}/app/libs/x86_64/libarcsoft_dualcam_refocus.so)

target_link_libraries( jni_arcsoft_real_bokeh libmpbase libarcsoft_dualcam_refocus ${log-lib} )