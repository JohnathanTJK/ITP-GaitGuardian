import cv2

# Load the video
video_path = "videos/408_color.mp4"  # Change this to your actual video file path
cap = cv2.VideoCapture(video_path)

if not cap.isOpened():
    print("Error: Cannot open video.")
    exit()

# Get total number of frames
total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
frame_num = 0

while cap.isOpened():
    # Set the video to the current frame position
    cap.set(cv2.CAP_PROP_POS_FRAMES, frame_num)

    # Read the current frame
    ret, frame = cap.read()
    if not ret:
        print("Reached end of video or error reading frame.")
        break

    # Display frame number on the frame (optional)
    label = f"Frame {frame_num + 1}/{total_frames}"
    cv2.putText(frame, label, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

    # Show the frame
    cv2.imshow("Frame Viewer", frame)

    # Wait for a key press
    key = cv2.waitKey(0) & 0xFF

    if key == ord('q'):
        print("Exiting viewer.")
        break
    elif key == ord('d'):  # next frame
        if frame_num < total_frames - 1:
            frame_num += 1
        else:
            print("End of video reached.")
    elif key == ord('a'):  # previous frame
        if frame_num > 0:
            frame_num -= 1
        else:
            print("Already at the first frame.")

# Cleanup
cap.release()
cv2.destroyAllWindows()
