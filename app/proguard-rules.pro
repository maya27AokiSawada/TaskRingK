# Add project specific ProGuard rules here.

# Keep Hilt generated components.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Firebase Firestore data models are mapped reflectively via toObject().
# Keep no-arg constructors and fields of domain/DTO classes used with Firestore.
-keepclassmembers class net.sumomo_planning.goshopping.data.remote.dto.** {
    <init>();
    <fields>;
}

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
