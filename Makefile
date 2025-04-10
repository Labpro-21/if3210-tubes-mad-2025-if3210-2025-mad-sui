
PACKAGE_NAME := com.vibecoder.purrytify
MAIN_ACTIVITY := MainActivity
AVD_NAME := mobdev
SYSTEM_IMAGE := system-images;android-34;google_apis;x86_64

ANDROID_HOME ?= $(HOME)/Android/Sdk
EMULATOR := $(ANDROID_HOME)/emulator/emulator
ADB := $(ANDROID_HOME)/platform-tools/adb
SDKMANAGER := $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager
AVDMANAGER := $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager
GRADLE := ./gradlew

.PHONY: help setup build install run emulator create-avd logs stop-emulator dev

help:
	@echo "Essential Purrytify Development Commands:"
	@echo "  make dev         - Complete development process (start emulator, build, run)"
	@echo "  make setup       - Install required Android SDK components"
	@echo "  make create-avd  - Create Android emulator"
	@echo "  make emulator    - Start emulator with keyboard input support"
	@echo "  make build       - Build the app"
	@echo "  make install     - Install the app on emulator/device"
	@echo "  make run         - Install and run the app"
	@echo "  make logs        - Show app logs"
	@echo "  make stop-emulator - Stop the emulator"

# Install essential SDK components
setup:
	@echo "Installing essential Android SDK components..."
	@yes | $(SDKMANAGER) --licenses
	@$(SDKMANAGER) "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator" "$(SYSTEM_IMAGE)"
	@echo "Setup completed. Now create an emulator with 'make create-avd'"

# Create AVD (emulator) with hardware keyboard support
create-avd:
	@echo "Creating AVD with hardware keyboard support..."
	@$(AVDMANAGER) create avd --name "$(AVD_NAME)" --package "$(SYSTEM_IMAGE)" --device "pixel_5"
	@mkdir -p $(HOME)/.android/avd/$(AVD_NAME).avd/
	@echo "hw.keyboard=yes" >> $(HOME)/.android/avd/$(AVD_NAME).avd/config.ini
	@echo "AVD created successfully with keyboard support enabled."

# Start emulator with optimized settings for development
emulator:
	@echo "Starting emulator..."
	@$(EMULATOR) -avd $(AVD_NAME) -gpu host -no-boot-anim &
	@echo "Waiting for emulator to boot..."
	@$(ADB) wait-for-device
	@sleep 8
	@echo "Emulator is ready."
	@echo "Enabling hardware keyboard..."
	@$(ADB) shell settings put secure show_ime_with_hard_keyboard 0
	@echo "Disabling animations for better performance..."
	@$(ADB) shell settings put global window_animation_scale 0.0
	@$(ADB) shell settings put global transition_animation_scale 0.0
	@$(ADB) shell settings put global animator_duration_scale 0.0

# Build the app
build:
	@echo "Building debug version..."
	@$(GRADLE) assembleDebug --stacktrace
	@echo "Build completed."

# Install the app
install:
	@echo "Installing app to device/emulator..."
	@$(GRADLE) installDebug
	@echo "Installation completed."

# Install and run the app
run: install
	@echo "Starting app..."
	@$(ADB) shell am start -n $(PACKAGE_NAME)/.$(MAIN_ACTIVITY)
	@echo "App started."

# Show app logs
logs:
	@echo "Showing logs for $(PACKAGE_NAME)..."
	@$(ADB) logcat '*:E' | grep -i "$(PACKAGE_NAME)"

# Stop the emulator
stop-emulator:
	@echo "Stopping emulator..."
	@$(ADB) emu kill
	@echo "Emulator stopped."

# Quick check connected devices
devices:
	@echo "Checking connected devices..."
	@$(ADB) devices -l

watch:
	@echo "Starting smart watch mode. Press Ctrl+C to stop."
	@echo "Watching for changes in app/src directory..."
	@while true; do \
		inotifywait -r -e modify,create,delete app/src; \
		echo "Changes detected! Rebuilding..."; \
		$(GRADLE) installDebug; \
		if [ -z "$$($(ADB) shell pidof $(PACKAGE_NAME))" ]; then \
			echo "App not running, starting it..."; \
			$(ADB) shell am start -n $(PACKAGE_NAME)/.$(MAIN_ACTIVITY); \
		else \
			echo "App already running, sending refresh broadcast..."; \
			$(ADB) shell am broadcast -a com.vibecoder.purrytify.REFRESH; \
		fi; \
		echo "Update complete."; \
	done
