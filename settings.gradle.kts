import dev.scaffoldit.hytale.Patchline

rootProject.name = "trigger-camera-plugin"

plugins {
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    usePatchline(Patchline.RELEASE.name)
    useVersion("0.5.7")
}