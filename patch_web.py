import sys

filepath = 'C:\\Users\\user\\.gemini\\antigravity\\scratch\\AlarmAppBackend\\src\\main\\resources\\static\\index.html'
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update CSS
old_css = "/* Block Editor Styles */"
new_css = """        /* Block Editor Styles */
        .block-gap { height: 16px; margin-bottom: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: transparent; transition: 0.2s; border-radius: 4px; }
        .block-gap:hover { background-color: rgba(0,229,255,0.1); color: #00E5FF; }
        .block { position: relative; margin-bottom: 8px; display: flex; align-items: flex-start; }
        .drag-handle { cursor: grab; padding: 4px 8px; color: #64748B; user-select: none; }
        .drag-handle:active { cursor: grabbing; }"""
content = content.replace(old_css, new_css)

# 2. Add Toolbar dynamic formatting container
old_toolbar_html = """        <div class="editor-toolbar">
            <button onclick="addTextBlock()">+ 텍스트 블록</button>
            <button onclick="triggerImageUpload()">+ 이미지 블록</button>
            <input type="file" id="image-upload" accept="image/*" class="hidden" onchange="uploadImage(event)">
        </div>"""
new_toolbar_html = """        <div class="editor-toolbar" style="flex-direction: column;">
            <div id="format-toolbar" style="display: none; width: 100%; gap: 8px; margin-bottom: 8px; justify-content: center;">
                <!-- dynamic formatting buttons -->
            </div>
            <div style="display: flex; width: 100%; gap: 12px;">
                <button onclick="addTextBlock()">+ 텍스트 블록</button>
                <button onclick="triggerImageUpload()">+ 이미지 블록</button>
                <input type="file" id="image-upload" accept="image/*" class="hidden" onchange="uploadImage(event)">
            </div>
        </div>"""
content = content.replace(old_toolbar_html, new_toolbar_html)

# 3. Add drag-and-drop & styling logic inside script
old_script_globals = "let editingBlocks = [];"
new_script_globals = "let editingBlocks = [];\n    let activeBlockIndex = null;\n    let draggedIndex = null;"
content = content.replace(old_script_globals, new_script_globals)

# 4. Replace renderBlocks completely
old_renderBlocks = content[content.find("function renderBlocks() {"):content.find("function addTextBlock() {")]
new_renderBlocks = """function updateFormatToolbar() {
        const tb = document.getElementById('format-toolbar');
        if (activeBlockIndex === null || !editingBlocks[activeBlockIndex]) {
            tb.style.display = 'none';
            return;
        }
        tb.style.display = 'flex';
        const block = editingBlocks[activeBlockIndex];
        
        if (block.type === 'text') {
            const isBold = block.isBold ? 'color:#00E5FF;' : 'color:#E2E8F0;';
            tb.innerHTML = 
                <button style="width:auto;  font-weight:bold;" onclick="toggleBold()">B</button>
                <button style="width:auto;" onclick="changeFontSize(-2)">A-</button>
                <button style="width:auto;" onclick="changeFontSize(2)">A+</button>
            ;
        } else if (block.type === 'image') {
            tb.innerHTML = 
                <span style="color:#FFF; line-height:36px; padding:0 8px;">크기:</span>
                <button style="width:auto;" onclick="changeWidthScale(-0.1)">-</button>
                <button style="width:auto;" onclick="changeWidthScale(0.1)">+</button>
            ;
        }
    }

    function toggleBold() {
        if(activeBlockIndex !== null) {
            editingBlocks[activeBlockIndex].isBold = !editingBlocks[activeBlockIndex].isBold;
            renderBlocks();
            updateFormatToolbar();
        }
    }
    function changeFontSize(delta) {
        if(activeBlockIndex !== null) {
            let s = editingBlocks[activeBlockIndex].fontSize || 18;
            s += delta;
            if(s < 10) s = 10;
            if(s > 48) s = 48;
            editingBlocks[activeBlockIndex].fontSize = s;
            renderBlocks();
        }
    }
    function changeWidthScale(delta) {
        if(activeBlockIndex !== null) {
            let w = editingBlocks[activeBlockIndex].widthScale || 1.0;
            w += delta;
            if(w < 0.3) w = 0.3;
            if(w > 1.0) w = 1.0;
            editingBlocks[activeBlockIndex].widthScale = w;
            renderBlocks();
        }
    }

    function handleDragStart(e, idx) { draggedIndex = idx; e.dataTransfer.effectAllowed = 'move'; e.target.style.opacity = '0.5'; }
    function handleDragOver(e) { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; }
    function handleDrop(e, idx) {
        e.preventDefault();
        if (draggedIndex !== null && draggedIndex !== idx) {
            const temp = editingBlocks[draggedIndex];
            editingBlocks[draggedIndex] = editingBlocks[idx];
            editingBlocks[idx] = temp;
            if(activeBlockIndex === draggedIndex) activeBlockIndex = idx;
            else if(activeBlockIndex === idx) activeBlockIndex = draggedIndex;
            renderBlocks();
            updateFormatToolbar();
        }
    }
    function handleDragEnd(e) { e.target.style.opacity = '1'; draggedIndex = null; }

    function insertBlockAt(index) {
        editingBlocks.splice(index, 0, { type: 'text', content: '', fontSize: 18, isBold: false, widthScale: 1.0 });
        activeBlockIndex = index;
        renderBlocks();
        updateFormatToolbar();
    }

    function renderBlocks() {
        const container = document.getElementById('memo-blocks');
        container.innerHTML = '';
        
        editingBlocks.forEach((block, index) => {
            // Gap for insertion
            const gap = document.createElement('div');
            gap.className = 'block-gap';
            gap.innerHTML = '+ 여기에 추가';
            gap.onclick = () => insertBlockAt(index);
            container.appendChild(gap);

            const div = document.createElement('div');
            div.className = 'block';
            div.draggable = true;
            div.ondragstart = (e) => handleDragStart(e, index);
            div.ondragover = handleDragOver;
            div.ondrop = (e) => handleDrop(e, index);
            div.ondragend = handleDragEnd;
            
            const handle = document.createElement('div');
            handle.className = 'drag-handle';
            handle.innerHTML = '≡';
            div.appendChild(handle);
            
            const contentDiv = document.createElement('div');
            contentDiv.className = 'block-content';
            contentDiv.onclick = () => { activeBlockIndex = index; updateFormatToolbar(); };
            
            if (block.type === 'text') {
                const textarea = document.createElement('textarea');
                textarea.value = block.content;
                textarea.placeholder = '내용을 입력하세요...';
                textarea.style.fontSize = (block.fontSize || 18) + 'px';
                textarea.style.fontWeight = block.isBold ? 'bold' : 'normal';
                
                textarea.oninput = (e) => { 
                    block.content = e.target.value; 
                    autoResize(e.target);
                };
                textarea.onkeydown = (e) => {
                    if (e.key === 'Backspace' && textarea.value === '' && editingBlocks.length > 1) {
                        e.preventDefault();
                        removeBlock(index);
                    }
                };
                textarea.onfocus = () => { activeBlockIndex = index; updateFormatToolbar(); };
                setTimeout(() => autoResize(textarea), 0);
                contentDiv.appendChild(textarea);
            } else if (block.type === 'image') {
                const img = document.createElement('img');
                const path = block.content;
                const filename = path.split('/').pop();
                img.src = /api/images/;
                img.style.width = ((block.widthScale || 1.0) * 100) + '%';
                img.onerror = () => { img.src = ''; img.alt = '이미지 로드 실패: ' + filename; img.style.border = '1px solid #475569'; img.style.padding = '20px'; };
                contentDiv.appendChild(img);
            }

            const actions = document.createElement('div');
            actions.className = 'block-actions';
            actions.innerHTML = <button onclick="removeBlock()" class="danger" title="블록 삭제">✕</button>;
            
            div.appendChild(contentDiv);
            div.appendChild(actions);
            container.appendChild(div);
        });

        // Final Gap
        const finalGap = document.createElement('div');
        finalGap.className = 'block-gap';
        finalGap.innerHTML = '+ 마지막에 추가';
        finalGap.onclick = () => insertBlockAt(editingBlocks.length);
        container.appendChild(finalGap);
    }
"""

content = content.replace(old_renderBlocks, new_renderBlocks)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Updated index.html")
