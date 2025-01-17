//
//  TunnelManager.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2023/12/25.
//

import Foundation
import NetworkExtension

protocol TunnelStatusDelegate: NSObjectProtocol {
    func onStatusChange(status: NEVPNStatus)
}

public class TunnelManager {
    
    var providerMgr: NETunnelProviderManager? = nil
    weak var statusDelegate: TunnelStatusDelegate? = nil
    
    func startService() {
        print("TunnelManager startService")
        if providerMgr != nil {
            enableConnection(mgr: providerMgr!)
        } else {
            loadTunnelProviderMgr()
        }
    }
    
    private func loadTunnelProviderMgr() {
        print("TunnelManager loadTunnelProviderMgr")
        NETunnelProviderManager.loadAllFromPreferences { [self] mgrs, err in
            if err != nil {
                print("NETunnelProviderManager.loadTunnelProviderMgr err")
                return
            }
            if mgrs!.count > 0 {
                print("TunnelManager has previous manager")
                enableConnection(mgr: mgrs!.first!)
            } else {
                print("TunnelManager has no manager, to create one")
                let mgr = NETunnelProviderManager()
                mgr.loadFromPreferences { [self] err in
                    if err != nil {
                        print("mgr.loadFromPreferences err")
                        return
                    }
                    let config = NETunnelProviderProtocol()
                    config.providerBundleIdentifier = "\(Bundle.main.bundleIdentifier!).TunnelProvider"
                    config.serverAddress = "20.2.219.253"
                    config.includeAllNetworks = true
                    config.providerConfiguration = [
                        "port": 22333
                    ]
                    mgr.protocolConfiguration = config
                    mgr.localizedDescription = "Spear"
                    mgr.saveToPreferences { [self] err in
                        if err != nil {
                            print("mgr.saveToPreferences err")
                        } else {
                            enableConnection(mgr: mgr)
                        }
                    }
                }
            }
        }
    }
    
    private func enableConnection(mgr: NETunnelProviderManager) {
        print("TunnelManager enableConnection")
        providerMgr = mgr
        mgr.isEnabled = true
        
        print("mgr.saveToPreferences before")
        mgr.saveToPreferences { err in
            print("mgr.saveToPreferences enter")
            if err != nil {
                print("mgr.saveToPreferences err")
                return
            }
            if mgr.connection.status == .disconnected {
                do {
                    print("mgr.connection.startVPNTunnel enter")
                    try mgr.connection.startVPNTunnel()
                    print("mgr.connection.startVPNTunnel success")
                } catch {
                    print("mgr.connection.startVPNTunnel failed: \(error.localizedDescription)")
                }
            } else {
                print("mgr.connection.startVPNTunnel failed, status is: \(mgr.connection.status)")
            }
        }
    }
    
}
