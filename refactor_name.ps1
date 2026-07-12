$targetDir = "g:\Hackhathon"

# 1. Rename the source package folder
$oldPath = Join-Path $targetDir "src\main\java\com\hackhathon"
$newPath = Join-Path $targetDir "src\main\java\com\hackhaton"

if (Test-Path $oldPath) {
    Rename-Item -Path $oldPath -NewName "hackhaton" -Force
    Write-Host "Renamed package folder to 'hackhaton'" -ForegroundColor Green
}

# 2. Text replace in all Java, XML, and properties files
$files = Get-ChildItem -Path $targetDir -Recurse -File | Where-Object { $_.Extension -match "\.(java|xml|properties|md)$" }

$count = 0
foreach ($file in $files) {
    if (!$file.FullName.Contains("\target\") -and !$file.FullName.Contains("\.git\")) {
        $content = Get-Content $file.FullName -Raw
        if ($content -match "(?i)hackhathon") {
            # Case-sensitive replacements to keep capitalization correct
            $newContent = $content -creplace "hackhathon", "hackhaton" -creplace "Hackhathon", "Hackhaton"
            [IO.File]::WriteAllText($file.FullName, $newContent)
            $count++
        }
    }
}
Write-Host "Updated project name inside $count files. Setup is complete!" -ForegroundColor Green
