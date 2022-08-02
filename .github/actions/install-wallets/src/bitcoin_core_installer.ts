import * as core from "@actions/core";
import * as tc from "@actions/tool-cache";
import path from "node:path";
import os from "node:os";
import fs from 'fs';
import {Installer} from "./installer";

export class BitcoinCoreInstaller implements Installer {
    programName: string;
    installDir: string;
    versionPropertyId: string;

    constructor(programName: string = 'Bitcoin Core',
                installDir: string = 'tools/bitcoin-core',
                versionPropertyId: string = 'bitcoin-core-version') {
        this.programName = programName;
        this.installDir = installDir;
        this.versionPropertyId = versionPropertyId;
    }

    async install() {
        const bitcoinVersion = core.getInput(this.versionPropertyId, {required: true});

        if (!this.isCacheHit(bitcoinVersion)) {
            const urlPrefix = this.getUrlPrefix(bitcoinVersion);
            let url = this.appendUrlSuffixForOs(urlPrefix)
            await this.downloadAndUnpackArchive(url);
        }

        const binDirPath = this.getBinDir(bitcoinVersion);
        core.addPath(binDirPath);
    }

    isCacheHit(version: string): boolean {
        const binDirPath = this.getBinDir(version);
        return fs.existsSync(binDirPath)
    }

    getUrlPrefix(version: string): string {
        return `https://bitcoin.org/bin/bitcoin-core-${version}/bitcoin-${version}-`;
    }

    appendUrlSuffixForOs(url_prefix: string): string {
        const platform = os.platform();
        switch (platform) {
            case "linux":
                return url_prefix + 'x86_64-linux-gnu.tar.gz';
            case "win32":
                return url_prefix + 'win64.zip';
            case "darwin":
                return url_prefix + 'osx64.tar.gz';
            default:
                throw 'Unknown OS';
        }
    }

    async downloadAndUnpackArchive(url: string) {
        console.log("Downloading: " + url)
        const bitcoinCorePath = await tc.downloadTool(url);

        if (url.endsWith('.tar.gz')) {
            return await tc.extractTar(bitcoinCorePath, this.installDir)
        } else if (url.endsWith('.zip')) {
            return await tc.extractZip(bitcoinCorePath, this.installDir)
        } else {
            throw 'Unknown archive format.'
        }
    }

    getBinDir(version: string): string {
        const unpackedTargetDir = this.getUnpackedTargetDirName(version);
        return path.join(this.installDir, unpackedTargetDir, 'bin');
    }

    getUnpackedTargetDirName(version: string): string {
        return `bitcoin-${version}`;
    }
}