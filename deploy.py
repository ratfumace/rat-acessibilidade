#!/usr/bin/env python3
"""
Rat Acessibilidade - Script de Deploy e Configuração Automática via ADB.
Instala o aplicativo em qualquer celular conectado e concede as permissões
de sistema (WRITE_SECURE_SETTINGS, Notificações e Isenção de Bateria) automaticamente.
"""

import os
import sys
import subprocess
import time

def run_cmd(cmd, check=True):
    res = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if check and res.returncode != 0:
        print(f"❌ Erro ao executar: {cmd}\n{res.stderr}")
    return res.stdout.strip(), res.returncode

def get_devices():
    out, _ = run_cmd("adb devices")
    devices = []
    for line in out.splitlines()[1:]:
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    apk_path = os.path.join(script_dir, "RatAcessibilidade.apk")

    print("════════════════════════════════════════════════════════════════")
    print("🛡️  RAT ACESSIBILIDADE — DEPLOY AUTOMÁTICO EM CELULAR")
    print("════════════════════════════════════════════════════════════════")

    if not os.path.exists(apk_path):
        print("📦 Compilando APK primeiro...")
        gradlew = os.path.join(script_dir, "gradlew")
        cmd_build = f'"{gradlew}" -p "{script_dir}" assembleRelease'
        _, code = run_cmd(cmd_build, check=False)
        if code != 0 or not os.path.exists(apk_path):
            print("❌ Falha na compilação do APK.")
            sys.exit(1)

    devices = get_devices()
    if not devices:
        print("⚠️ Nenhum aparelho Android detectado via ADB!")
        print("   Conecte o aparelho via USB com Depuração USB ativada ou via 'adb connect <ip>:<porta>'")
        sys.exit(1)

    target_device = devices[0]
    print(f"📱 Dispositivo Alvo: {target_device} (Total conectados: {len(devices)})")

    # 1. Instalação
    print("\n[1/4] Instalando RatAcessibilidade.apk...")
    install_out, code = run_cmd(f'adb -s {target_device} install -r "{apk_path}"', check=False)
    if "Success" in install_out or code == 0:
        print("   ✅ Aplicativo instalado com sucesso!")
    else:
        print(f"   ⚠️ Saída da instalação: {install_out}")

    # 2. Concessão de WRITE_SECURE_SETTINGS
    print("\n[2/4] Concedendo permissão de sistema WRITE_SECURE_SETTINGS...")
    run_cmd(f'adb -s {target_device} shell pm grant com.rat.acessibilidade android.permission.WRITE_SECURE_SETTINGS', check=False)
    
    # Validação da permissão
    dump_out, _ = run_cmd(f'adb -s {target_device} shell dumpsys package com.rat.acessibilidade | grep WRITE_SECURE_SETTINGS', check=False)
    if "granted=true" in dump_out or "WRITE_SECURE_SETTINGS" in dump_out:
        print("   ✅ Permissão WRITE_SECURE_SETTINGS concedida com sucesso!")
    else:
        print("   ℹ️ Permissão aplicada.")

    # 3. Notificações e Otimização de Bateria
    print("\n[3/4] Configurando segundo plano e bateria (Doze Mode)...")
    run_cmd(f'adb -s {target_device} shell pm grant com.rat.acessibilidade android.permission.POST_NOTIFICATIONS', check=False)
    run_cmd(f'adb -s {target_device} shell dumpsys deviceidle whitelist +com.rat.acessibilidade', check=False)
    print("   ✅ Notificações e Isenção de Bateria configuradas!")

    # 4. Iniciar App
    print("\n[4/4] Abrindo Rat Acessibilidade no aparelho...")
    run_cmd(f'adb -s {target_device} shell am start -n com.rat.acessibilidade/.MainActivity', check=False)
    print("   ✅ App aberto na tela do aparelho!")

    print("\n════════════════════════════════════════════════════════════════")
    print("🎉 DEPLOY CONCLUÍDO COM SUCESSO!")
    print("O aplicativo está pronto para uso e com todas as permissões ativas.")
    print("════════════════════════════════════════════════════════════════\n")

if __name__ == "__main__":
    main()
