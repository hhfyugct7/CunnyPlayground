import tkinter as tk
from tkinter import ttk, scrolledtext
import subprocess
import os

class HyperPlaygroundApp:
    def __init__(self, root):
        self.root = root
        self.root.title("HyperIsland Desktop Playground")
        self.root.geometry("900x750")
        self.root.configure(bg="#121212")
        
        self.style = ttk.Style()
        self.style.theme_use('clam')
        self.setup_styles()
        
        # Main Layout
        self.main_frame = tk.Frame(self.root, bg="#121212", padx=20, pady=20)
        self.main_frame.pack(fill=tk.BOTH, expand=True)
        
        # Device Info Section
        self.device_info_label = tk.Label(
            self.main_frame, 
            text="Device information: Fetching...", 
            fg="#6FCF97", 
            bg="#121212",
            font=("Segoe UI", 11, "bold"),
            justify=tk.LEFT,
            anchor="w"
        )
        self.device_info_label.pack(fill=tk.X, pady=(0, 15))
        
        # JSON Input Section
        tk.Label(self.main_frame, text="JSON Payload Editor", fg="#d4d4d4", bg="#121212", font=("Segoe UI", 10)).pack(anchor="w")
        self.json_editor = scrolledtext.ScrolledText(
            self.main_frame, 
            height=18, 
            bg="#1e1e1e", 
            fg="#d4d4d4", 
            insertbackground="white",
            font=("Consolas", 11),
            padx=10,
            pady=10,
            borderwidth=0,
            highlightthickness=1,
            highlightbackground="#333333",
            highlightcolor="#4A90E2"
        )
        self.json_editor.pack(fill=tk.BOTH, expand=True, pady=(5, 15))
        self.json_editor.insert(tk.END, "{\n    \"param_v2\": {\n        \"protocol\": 3,\n        \"business\": \"code\",\n        \"updatable\": true,\n        \"ticker\": \"Code\",\n        \"isShowNotification\": true\n    }\n}")

        # Dumpsys Monitor Section
        tk.Label(self.main_frame, text="Notification Dumpsys (ID: 80085)", fg="#888888", bg="#121212", font=("Segoe UI", 10)).pack(anchor="w")
        self.dumpsys_monitor = scrolledtext.ScrolledText(
            self.main_frame, 
            height=8, 
            bg="#0f0f0f", 
            fg="#aaaaaa", 
            font=("Consolas", 10),
            padx=10,
            pady=10,
            state=tk.DISABLED,
            borderwidth=0,
            highlightthickness=1,
            highlightbackground="#222222"
        )
        self.dumpsys_monitor.pack(fill=tk.BOTH, expand=False, pady=(5, 20))
        
        # Footer Action Bar
        self.btn_send = tk.Button(
            self.main_frame, 
            text="Send Payload", 
            command=self.send_payload,
            bg="#4A90E2", 
            fg="white", 
            font=("Segoe UI", 12, "bold"),
            padx=30,
            pady=10,
            borderwidth=0,
            activebackground="#357ABD",
            cursor="hand2"
        )
        self.btn_send.pack()
        
        # Initial Fetch
        self.update_device_info()
        self.refresh_dumpsys()

    def setup_styles(self):
        self.style.configure("TFrame", background="#121212")
        self.style.configure("TLabel", background="#121212", foreground="#d4d4d4")
        
    def run_adb(self, cmd):
        try:
            result = subprocess.run(f"adb {cmd}", shell=True, capture_output=True, text=True)
            return result.stdout.strip()
        except Exception as e:
            return f"Error: {str(e)}"

    def update_device_info(self):
        device = self.run_adb("shell getprop ro.product.marketname")
        os_version = self.run_adb("shell getprop ro.mi.os.version.incremental")
        android_version = self.run_adb("shell getprop ro.build.version.release")
        
        if not device: device = "Unknown Device"
        if not os_version: os_version = "N/A"
        if not android_version: android_version = "N/A"
        
        info = f"Device information:\nDevice: {device}, OS: {os_version}, Android: {android_version}"
        self.device_info_label.config(text=info)

    def refresh_dumpsys(self):
        output = self.run_adb("shell dumpsys notification --nocompress | findstr 80085")
        self.dumpsys_monitor.config(state=tk.NORMAL)
        self.dumpsys_monitor.delete('1.0', tk.END)
        if output:
            self.dumpsys_monitor.insert(tk.END, output)
        else:
            self.dumpsys_monitor.insert(tk.END, "Notification ID 80085 not found in dumpsys.")
        self.dumpsys_monitor.config(state=tk.DISABLED)

    def send_payload(self):
        payload = self.json_editor.get("1.0", tk.END).strip()
        temp_path = "C:/Users/Adam/notif.json"
        
        try:
            with open(temp_path, "w", encoding="utf-8") as f:
                f.write(payload)
                
            # ADB Sequence
            self.run_adb(f"push {temp_path} /data/local/tmp/notif.json")
            self.run_adb("shell am broadcast -n com.thevakhovske.cunnyplayground/.HyperCommandReceiver -a com.thevakhovske.cunnyplayground.SEND_HYPER --es file \"/data/local/tmp/notif.json\"")
            
            # Update Log
            self.refresh_dumpsys()
        except Exception as e:
            self.dumpsys_monitor.config(state=tk.NORMAL)
            self.dumpsys_monitor.insert(tk.END, f"\n[LOCAL ERROR]: {str(e)}")
            self.dumpsys_monitor.config(state=tk.DISABLED)

if __name__ == "__main__":
    root = tk.Tk()
    app = HyperPlaygroundApp(root)
    root.mainloop()
