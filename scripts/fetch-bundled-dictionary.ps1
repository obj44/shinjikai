param(
    [string] $Ref = "main",
    [switch] $RawJsonl,
    [switch] $KeepArchive,
    [int] $ImageMaxDimension = 1280,
    [int] $ImageQuality = 72,
    [long] $ImageChunkBytes = 67108864
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$assetRoot = Join-Path $repoRoot "app/src/main/assets/bundled_dictionary"
$scratchRoot = Join-Path $repoRoot "scratch/bundled-dictionary"
$archivePath = Join-Path $scratchRoot "1Selxo-Shinjikai-$Ref.zip"
$extractRoot = Join-Path $scratchRoot "expanded"
$compressScript = Join-Path $scratchRoot "compress-jsonl-xz.py"
$imageCompressScript = Join-Path $scratchRoot "compress-images.py"
$archiveUrl = "https://codeload.github.com/1Selxo/Shinjikai/zip/refs/heads/$Ref"

function Assert-PathUnder {
    param(
        [Parameter(Mandatory = $true)] [string] $Path,
        [Parameter(Mandatory = $true)] [string] $Root
    )

    $rootFull = [System.IO.Path]::GetFullPath($Root)
    $pathFull = [System.IO.Path]::GetFullPath($Path)
    if (-not $pathFull.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside expected root: $pathFull"
    }
}

function Split-FileIntoChunks {
    param(
        [Parameter(Mandatory = $true)] [string] $SourcePath,
        [Parameter(Mandatory = $true)] [string] $DestinationDirectory,
        [Parameter(Mandatory = $true)] [string] $OutputPrefix,
        [Parameter(Mandatory = $true)] [long] $ChunkBytes
    )

    $buffer = New-Object byte[] (1024 * 1024)
    $sourceStream = [System.IO.File]::OpenRead($SourcePath)
    try {
        $chunkIndex = 0
        while ($sourceStream.Position -lt $sourceStream.Length) {
            $chunkName = "{0}.part{1:D3}.tar.xz" -f $OutputPrefix, $chunkIndex
            $chunkPath = Join-Path $DestinationDirectory $chunkName
            $chunkStream = [System.IO.File]::Create($chunkPath)
            try {
                $written = [int64] 0
                while ($written -lt $ChunkBytes -and $sourceStream.Position -lt $sourceStream.Length) {
                    $maxRead = [Math]::Min($buffer.Length, $ChunkBytes - $written)
                    $read = $sourceStream.Read($buffer, 0, [int] $maxRead)
                    if ($read -le 0) {
                        break
                    }
                    $chunkStream.Write($buffer, 0, $read)
                    $written += $read
                }
            } finally {
                $chunkStream.Dispose()
            }
            $chunkIndex += 1
        }
    } finally {
        $sourceStream.Dispose()
    }
}

Assert-PathUnder -Path $assetRoot -Root $repoRoot
Assert-PathUnder -Path $scratchRoot -Root $repoRoot

New-Item -ItemType Directory -Force -Path $scratchRoot | Out-Null

Write-Host "Downloading $archiveUrl"
curl.exe -L --fail $archiveUrl -o $archivePath

if (Test-Path -LiteralPath $extractRoot) {
    Assert-PathUnder -Path $extractRoot -Root $scratchRoot
    Remove-Item -LiteralPath $extractRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null

Write-Host "Extracting archive"
Expand-Archive -LiteralPath $archivePath -DestinationPath $extractRoot -Force

$expandedRoot = Get-ChildItem -LiteralPath $extractRoot -Directory | Select-Object -First 1
if ($null -eq $expandedRoot) {
    throw "Downloaded archive did not contain a repository folder."
}

$sourceData = Join-Path $expandedRoot.FullName "shinjikai_data"
$sourceImages = Join-Path $expandedRoot.FullName "yomitan_images"
if (-not (Test-Path -LiteralPath $sourceData)) {
    throw "Archive is missing shinjikai_data."
}
if (-not (Test-Path -LiteralPath $sourceImages)) {
    throw "Archive is missing yomitan_images."
}

if (Test-Path -LiteralPath $assetRoot) {
    Assert-PathUnder -Path $assetRoot -Root $repoRoot
    Remove-Item -LiteralPath $assetRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $assetRoot | Out-Null
$targetData = Join-Path $assetRoot "shinjikai_data"
New-Item -ItemType Directory -Force -Path $targetData | Out-Null

if ($RawJsonl) {
    Get-ChildItem -LiteralPath $sourceData -File -Filter "*.jsonl" | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $targetData
    }
} else {
    $python = Get-Command python -ErrorAction SilentlyContinue
    $pythonArgs = @()
    if ($null -eq $python) {
        $python = Get-Command py -ErrorAction SilentlyContinue
        $pythonArgs = @("-3")
    }
    if ($null -eq $python) {
        throw "Python is required to create .jsonl.xz assets. Re-run with -RawJsonl to copy uncompressed JSONL instead."
    }

    @'
import lzma
import pathlib
import shutil
import sys

source_dir = pathlib.Path(sys.argv[1])
target_dir = pathlib.Path(sys.argv[2])
target_dir.mkdir(parents=True, exist_ok=True)
preset = 9 | lzma.PRESET_EXTREME

for source in sorted(source_dir.glob("*.jsonl")):
    target = target_dir / f"{source.name}.xz"
    print(f"Compressing {source.name} -> {target.name}", flush=True)
    with source.open("rb") as raw, lzma.open(target, "wb", preset=preset) as compressed:
        shutil.copyfileobj(raw, compressed, length=1024 * 1024)
'@ | Set-Content -LiteralPath $compressScript -Encoding UTF8

    & $python.Source @pythonArgs $compressScript $sourceData $targetData
}

$targetImages = Join-Path $assetRoot "yomitan_images"
$imageArchivePath = Join-Path $assetRoot "yomitan_images.tar.xz"
Copy-Item -LiteralPath $sourceImages -Destination $targetImages -Recurse

$imagePython = Get-Command python -ErrorAction SilentlyContinue
$imagePythonArgs = @()
if ($null -eq $imagePython) {
    $imagePython = Get-Command py -ErrorAction SilentlyContinue
    $imagePythonArgs = @("-3")
}

if ($null -ne $imagePython) {
    & $imagePython.Source @imagePythonArgs -c "import importlib.util; raise SystemExit(0 if importlib.util.find_spec('PIL') else 1)" 2>$null
    $hasPillow = $LASTEXITCODE -eq 0
    if ($hasPillow) {
        @'
from pathlib import Path
from PIL import Image, ImageOps
import os
import sys

root = Path(sys.argv[1])
max_dim = int(sys.argv[2])
quality = int(sys.argv[3])
before = 0
after = 0
changed = 0
skipped = 0

for path in root.rglob("*"):
    if not path.is_file():
        continue
    suffix = path.suffix.lower()
    original_size = path.stat().st_size
    before += original_size
    if suffix not in {".jpg", ".jpeg", ".png", ".webp"}:
        after += original_size
        skipped += 1
        continue

    tmp = path.with_suffix(path.suffix + ".tmp")
    try:
        with Image.open(path) as im:
            im = ImageOps.exif_transpose(im)
            if max(im.size) > max_dim:
                im.thumbnail((max_dim, max_dim), Image.Resampling.LANCZOS)
            if suffix in {".jpg", ".jpeg"}:
                if im.mode not in ("RGB", "L"):
                    im = im.convert("RGB")
                im.save(tmp, format="JPEG", quality=quality, optimize=True, progressive=True, subsampling=2)
            elif suffix == ".webp":
                im.save(tmp, format="WEBP", quality=quality, method=6)
            else:
                im.save(tmp, format="PNG", optimize=True, compress_level=9)
        new_size = tmp.stat().st_size
        if new_size < original_size:
            os.replace(tmp, path)
            changed += 1
            after += new_size
        else:
            tmp.unlink(missing_ok=True)
            skipped += 1
            after += original_size
    except Exception as exc:
        tmp.unlink(missing_ok=True)
        skipped += 1
        after += original_size
        print(f"Skipped {path}: {type(exc).__name__}: {exc}", flush=True)

print(f"Image bytes before: {before}", flush=True)
print(f"Image bytes after: {after}", flush=True)
print(f"Image bytes saved: {before - after}", flush=True)
print(f"Images recompressed: {changed}", flush=True)
print(f"Images left unchanged: {skipped}", flush=True)
'@ | Set-Content -LiteralPath $imageCompressScript -Encoding UTF8

        & $imagePython.Source @imagePythonArgs $imageCompressScript $targetImages $ImageMaxDimension $ImageQuality
    } else {
        Write-Warning "Python is available but Pillow is not installed; image pixels will be archived without recompression."
    }
} else {
    Write-Warning "Python is not available; image pixels will be archived without recompression."
}

Get-ChildItem -LiteralPath $assetRoot -File -Filter "yomitan_images*.tar.xz" -ErrorAction SilentlyContinue |
    Remove-Item -Force
Write-Host "Packing images -> yomitan_images.tar.xz"
tar -cJf $imageArchivePath -C $assetRoot "yomitan_images"
Remove-Item -LiteralPath $targetImages -Recurse -Force

Write-Host "Splitting image archive into <$ImageChunkBytes byte git-safe chunks"
Split-FileIntoChunks -SourcePath $imageArchivePath -DestinationDirectory $assetRoot -OutputPrefix "yomitan_images" -ChunkBytes $ImageChunkBytes
Remove-Item -LiteralPath $imageArchivePath -Force

$dataFiles = Get-ChildItem -LiteralPath (Join-Path $assetRoot "shinjikai_data") -File | Where-Object {
    $_.Name -like "*.jsonl" -or $_.Name -like "*.jsonl.xz" -or $_.Name -like "*.jsonl.gz"
}
$imageFiles = Get-ChildItem -LiteralPath $assetRoot -File -Filter "yomitan_images.part*.tar.xz" | Sort-Object Name
$dataBytes = ($dataFiles | Measure-Object Length -Sum).Sum
$imageBytes = ($imageFiles | Measure-Object Length -Sum).Sum

Write-Host "Bundled dictionary assets installed:"
Write-Host "  JSONL files: $($dataFiles.Count) ($dataBytes bytes)"
Write-Host "  Image archive chunks: $($imageFiles.Count) ($imageBytes bytes)"
Write-Host "  Text compression: $(if ($RawJsonl) { 'raw JSONL' } else { 'XZ preset 9 extreme' })"
Write-Host "  Image compression: recompress-if-smaller, max dimension $ImageMaxDimension, quality $ImageQuality, packed as chunked tar.xz"
Write-Host "  Target: $assetRoot"

if (-not $KeepArchive) {
    Remove-Item -LiteralPath $archivePath -Force
}

if (Test-Path -LiteralPath $extractRoot) {
    Remove-Item -LiteralPath $extractRoot -Recurse -Force
}
if (Test-Path -LiteralPath $compressScript) {
    Remove-Item -LiteralPath $compressScript -Force
}
if (Test-Path -LiteralPath $imageCompressScript) {
    Remove-Item -LiteralPath $imageCompressScript -Force
}
